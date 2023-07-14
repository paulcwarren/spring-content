package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;

import lombok.Getter;

/**
 * Returns a map of "path"'s to content properties for the given class.
 *
 * @author warrenpa
 *
 */
@Getter
public class ContentPropertyMappingContextVisitor implements ClassVisitor {

    private static final Log LOGGER = LogFactory.getLog(ContentPropertyMappingContextVisitor.class);

    private Map<Class<?>, Boolean> looseModes = new HashMap<>();
    private Map<String, Map<String,ContentProperty>> properties = new HashMap<>();
    private CharSequence keySeparator;
    private CharSequence contentPropertySeparator;

    public ContentPropertyMappingContextVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator) {
        this.keySeparator = keySeparator;
        this.contentPropertySeparator = contentPropertySeparator;
    }

    public Map<String,ContentProperty> getProperties() {
        Map<String,ContentProperty> props = new HashMap<>();
        for (Map<String,ContentProperty> property : properties.values()) {
            props.putAll(property);
        }

        return props;
    }

    @Override
    public boolean visitClass(String path, Class<?> klazz) {

        properties.put(key(path, klazz), new HashMap<String, ContentProperty>());

        int numContentIds = 0;
        for (Field field : klazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ContentId.class)) {
                numContentIds++;
            }
        }
        looseModes.put(klazz, numContentIds == 1);
        LOGGER.trace(String.format("Loose mode enabled: %s", looseModes.get(klazz)));
        return true;
    }

    private String key(String path, Class<?> klazz) {
        return klazz.getCanonicalName() + "." + path;
    }

    @Override
    public boolean visitClassEnd(String path, Class<?> klazz) {

        boolean looseMode = looseModes.get(klazz);
        Map<String,ContentProperty> props = properties.get(key(path, klazz));

        if (looseMode && props.size() >= 1) {

            LOGGER.trace(String.format("Loose mode enabled. Collapsing properties for %s", klazz.getCanonicalName()));

            ContentProperty contentProperty = new ContentProperty();
            for (ContentProperty property : props.values()) {
                if (property.getContentIdPropertyPath() != null) {
                    contentProperty.setContentPropertyPath(property.getContentPropertyPath());
                    contentProperty.setContentIdPropertyPath(property.getContentIdPropertyPath());
                    contentProperty.setContentIdType(property.getContentIdType());
                }
                if (property.getContentLengthPropertyPath() != null) {
                    contentProperty.setContentLengthPropertyPath(property.getContentLengthPropertyPath());
                }
                if (property.getMimeTypePropertyPath() != null) {
                    contentProperty.setMimeTypePropertyPath(property.getMimeTypePropertyPath());
                }
                if (property.getOriginalFileNamePropertyPath() != null) {
                    contentProperty.setOriginalFileNamePropertyPath(property.getOriginalFileNamePropertyPath());
                }
            }

            Map<String,ContentProperty> newClassProps = new HashMap<String,ContentProperty>();
            properties.put(key(path, klazz), newClassProps);

            if (isNotRootContentProperty(path)) {
                newClassProps.put(path, contentProperty);
            } else {
                newClassProps.put(contentProperty.getContentPropertyPath(), contentProperty);
            }
        }
        return true;
    }

    @Override
    public boolean visitField(String path, Class<?> klazz, Field f) {
        LOGGER.trace(String.format("Visiting %s.%s", f.getDeclaringClass().getCanonicalName(), f.getName()));

        if (f.isAnnotationPresent(ContentId.class)) {
            LOGGER.trace(String.format("%s.%s is @ContentId", f.getDeclaringClass().getCanonicalName(), f.getName()));
            String propertyName = fullyQualify(path, this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                Map<String,ContentProperty> classProperties = properties.get(key(path, klazz));
                ContentProperty property = classProperties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    classProperties.put(propertyName, property);
                }
                updateContentProperty(property::setContentPropertyPath, fullyQualify(path, this.propertyName(f.getName()), this.getContentPropertySeparator()));
                updateContentProperty(property::setContentIdPropertyPath, fullyQualify(path, f.getName(), this.getContentPropertySeparator()));
                property.setContentIdType(TypeDescriptor.valueOf(f.getType()));
            }
        } else if (f.isAnnotationPresent(ContentLength.class)) {
            LOGGER.trace(String.format("%s.%s is @ContentLength", f.getDeclaringClass().getCanonicalName(), f.getName()));
            String propertyName = fullyQualify(path, this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                Map<String,ContentProperty> classProperties = properties.get(key(path, klazz));
                ContentProperty property = classProperties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    classProperties.put(propertyName, property);
                }
                updateContentProperty(property::setContentLengthPropertyPath, fullyQualify(path, f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.isAnnotationPresent(MimeType.class)) {
            LOGGER.trace(String.format("%s.%s is @MimeType", f.getDeclaringClass().getCanonicalName(), f.getName()));
            String propertyName = fullyQualify(path, this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                Map<String,ContentProperty> classProperties = properties.get(key(path, klazz));
                ContentProperty property = classProperties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    classProperties.put(propertyName, property);
                }
                updateContentProperty(property::setMimeTypePropertyPath, fullyQualify(path, f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.isAnnotationPresent(OriginalFileName.class)) {
            LOGGER.trace(String.format("%s.%s is @OriginalFileName", f.getDeclaringClass().getCanonicalName(), f.getName()));
            String propertyName = fullyQualify(path, this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                Map<String,ContentProperty> classProperties = properties.get(key(path, klazz));
                ContentProperty property = classProperties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    classProperties.put(propertyName, property);
                }
                updateContentProperty(property::setOriginalFileNamePropertyPath, fullyQualify(path, f.getName(), this.getContentPropertySeparator()));
            }
        }

        return true;
    }

    protected void updateContentProperty(Consumer<String> propertyPathField, String propertyPath) {
        propertyPathField.accept(propertyPath);
    }

    protected String fullyQualify(String path, String name, CharSequence separator) {
        String fqName = name;
        if (StringUtils.hasLength(path) ) {
            String propertyPath = path.replaceAll("/", separator.toString());
            fqName = String.format("%s%s%s", propertyPath, separator, fqName);
        }
        return fqName;
    }

    protected String propertyName(String name) {
        if (!StringUtils.hasLength(name)) {
            return name;
        }

        String propertyName = calculateName(name);
        if (propertyName != null) {
            return propertyName;
        }

        String[] segments = split(name);
        if (segments.length == 1) {
            return segments[0];
        }
        else {
            StringBuilder b = new StringBuilder();
            for (int i=0; i < segments.length - 1; i++) {
                b.append(segments[i]);
            }
            return b.toString();
        }
    }

    protected boolean isNotRootContentProperty(String path) {
        return StringUtils.hasLength(path);
    }

    protected String calculateName(String name) {
        Pattern p = Pattern.compile("^(.+)(Id|Len|Length|MimeType|Mimetype|ContentType|(?<!Mime|Content)Type|(?<!Original)FileName|(?<!Original)Filename|OriginalFileName|OriginalFilename)$");
        Matcher m = p.matcher(name);
        if (m.matches() == false) {
            return null;
        }
        return m.group(1);
    }

    protected static String[] split(String name) {
        if (!StringUtils.hasLength(name)) {
            return new String[]{};
        }

        return name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }
}
