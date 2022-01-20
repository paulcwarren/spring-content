package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.util.StringUtils;

import lombok.Getter;

/**
 * Returns a map of "path"'s to content properties for the given class.
 *
 *
 *
 * @author warrenpa
 *
 */
@Getter
public class ContentPropertyBuilderVisitor {

    private Set<Field> fields = new HashSet<>();
    private Map<String, ContentProperty> properties = new HashMap<String,ContentProperty>();
    private Map<String, ContentProperty> subProperties = new HashMap<String,ContentProperty>();
    private CharSequence keySeparator;
    private CharSequence contentPropertySeparator;
    private NameProvider nameProvider;
    private boolean looseMode;

    public ContentPropertyBuilderVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator, NameProvider nameProvider) {
        this.keySeparator = keySeparator;
        this.contentPropertySeparator = contentPropertySeparator;
        this.nameProvider = nameProvider;
    }

    public boolean visitClass(Class<?> klazz) {
        int numContentIds = 0;
        for (Field field : klazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ContentId.class)) {
                numContentIds++;
            }
        }
        looseMode = numContentIds == 1;
        return true;
    }

    public boolean visitClassEnd(Class<?> klazz) {

        if (looseMode && properties.size() > 1) {

            ContentProperty contentProperty = new ContentProperty();
            for (ContentProperty property : this.properties.values()) {
                if (property.getContentIdPropertyPath() != null) {
                    contentProperty.setContentPropertyPath(property.getContentPropertyPath());
                    contentProperty.setContentIdPropertyPath(property.getContentIdPropertyPath());
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

            this.properties = new HashMap<String,ContentProperty>();
            this.properties.put(fullyQualify(propertyName(""), this.getKeySeparator()), contentProperty);
            this.properties.put(contentProperty.getContentPropertyPath().replace(this.getContentPropertySeparator(), this.getKeySeparator()), contentProperty);
        }

        for (Entry<String, ContentProperty> entry : subProperties.entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
        }

        return true;
    }

    boolean visitField(Field f) {

        if (f.isAnnotationPresent(ContentId.class)) {
            String propertyName = fullyQualify(this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                ContentProperty property = properties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    properties.put(propertyName, property);
                }
                updateContentProperty(property::setContentPropertyPath, fullyQualify(this.propertyName(f.getName()), this.getContentPropertySeparator()));
                updateContentProperty(property::setContentIdPropertyPath, fullyQualify(f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.isAnnotationPresent(ContentLength.class)) {
            String propertyName = fullyQualify(this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                ContentProperty property = properties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    properties.put(propertyName, property);
                }
                updateContentProperty(property::setContentLengthPropertyPath, fullyQualify(f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.isAnnotationPresent(MimeType.class)) {
            String propertyName = fullyQualify(this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                ContentProperty property = properties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    properties.put(propertyName, property);
                }
                updateContentProperty(property::setMimeTypePropertyPath, fullyQualify(f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.isAnnotationPresent(OriginalFileName.class)) {
            String propertyName = fullyQualify(this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                ContentProperty property = properties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    properties.put(propertyName, property);
                }
                updateContentProperty(property::setOriginalFileNamePropertyPath, fullyQualify(f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.getType().isPrimitive() == false && f.getType().equals(String.class) == false && ContentPropertyUtils.isWrapperType(f.getType()) == false) {
            ClassWalker subWalker = new ClassWalker(f.getType());
            ContentPropertyBuilderVisitor visitor = new SubClassVisitor(
                    this.keySeparator,
                    this.contentPropertySeparator,
                    this.nameProvider,
                    this instanceof SubClassVisitor ? (String[])ArrayUtils.add(((SubClassVisitor)this).propertyPathPrefix, f.getName()) : new String[] {f.getName()});
            subWalker.accept(visitor);
            for (Entry<String, ContentProperty> entry : visitor.getProperties().entrySet()) {
//                subProperties.put(f.getName() + this.keySeparator + entry.getKey(), entry.getValue());
                subProperties.put(entry.getKey().replace(this.getContentPropertySeparator(), this.getKeySeparator()), entry.getValue());

                if (visitor.getProperties().size() == 1) {
                    StringBuilder builder = new StringBuilder();
                    for (int i=0; i < ((SubClassVisitor)visitor).propertyPathPrefix.length; i++) {
                        if (i > 0) {
                            builder.append(this.getKeySeparator());
                        }
                        builder.append(((SubClassVisitor)visitor).propertyPathPrefix[i]);
                    }

                    subProperties.put(builder.toString(), entry.getValue());
                }
            }
        }

        return true;
    }

    protected void updateContentProperty(Consumer<String> propertyPathField, String propertyPath) {
        propertyPathField.accept(propertyPath);
    }

    protected String fullyQualify(String name, CharSequence separator) {
        return name;
    }

    protected String propertyName(String name) {
        if (!StringUtils.hasLength(name)) {
            return name;
        }
        String[] segments = split(name);
        return segments[0];
    }

    private static String[] split(String name) {
        if (!StringUtils.hasLength(name)) {
            return new String[]{};
        }

        return name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

    public static class CanonicalName implements NameProvider {

        @Override
        public String name(String fieldName) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SubClassVisitor extends ContentPropertyBuilderVisitor {

        private String[] propertyPathPrefix;

        public SubClassVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator, NameProvider nameProvider, String[] propertyPathPrefix) {
            super(keySeparator, contentPropertySeparator, nameProvider);
            this.propertyPathPrefix = propertyPathPrefix;
        }

        @Override
        protected String fullyQualify(String name, CharSequence separator) {
            StringBuilder builder = new StringBuilder();
            for (int i=0; i < this.propertyPathPrefix.length; i++) {
                if (i > 0) {
                    builder.append(separator);
                }
                builder.append(this.propertyPathPrefix[i]);
            }
            if (StringUtils.hasLength(name)) {
                builder.append(separator);
                builder.append(name);
            }
            return builder.toString();
        }
    }
}
