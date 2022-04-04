package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log LOGGER = LogFactory.getLog(ContentPropertyBuilderVisitor.class);

    private Set<Field> fields = new HashSet<>();
    private Map<String, ContentProperty> properties = new HashMap<String,ContentProperty>();
    private Map<String, ContentProperty> subProperties = new HashMap<String,ContentProperty>();
    private CharSequence keySeparator;
    private CharSequence contentPropertySeparator;
    private NameProvider nameProvider;
    private boolean looseMode;
    private List<Class<?>> classesVisited = new ArrayList<>();

    public ContentPropertyBuilderVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator, NameProvider nameProvider) {
        this.keySeparator = keySeparator;
        this.contentPropertySeparator = contentPropertySeparator;
        this.nameProvider = nameProvider;
    }

    /* package */ ContentPropertyBuilderVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator, NameProvider nameProvider, List<Class<?>> classesVisited) {
        this.keySeparator = keySeparator;
        this.contentPropertySeparator = contentPropertySeparator;
        this.nameProvider = nameProvider;
        this.classesVisited = classesVisited;
    }

    public boolean visitClass(Class<?> klazz) {

        if (classesVisited.contains(klazz)) {
            LOGGER.trace(String.format("Class %s already visited", klazz.getCanonicalName()));
            return false;
        } else {
            LOGGER.trace(String.format("Visiting class %s", klazz.getCanonicalName()));
            classesVisited.add(klazz);
        }

        int numContentIds = 0;
        for (Field field : klazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ContentId.class)) {
                numContentIds++;
            }
        }
        looseMode = numContentIds == 1;
        LOGGER.trace(String.format("Loose mode enabled: %s", looseMode));
        return true;
    }

    public boolean visitClassEnd(Class<?> klazz) {

        if (looseMode && properties.size() > 1) {

            LOGGER.trace(String.format("Loose mode enabled. Collapsing properties for %s", klazz.getCanonicalName()));

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
        LOGGER.trace(String.format("Visiting %s.%s", f.getDeclaringClass().getCanonicalName(), f.getName()));

        if (f.isAnnotationPresent(ContentId.class)) {
            LOGGER.trace(String.format("%s.%s is @ContentId", f.getDeclaringClass().getCanonicalName(), f.getName()));
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
            LOGGER.trace(String.format("%s.%s is @ContentLength", f.getDeclaringClass().getCanonicalName(), f.getName()));
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
            LOGGER.trace(String.format("%s.%s is @MimeType", f.getDeclaringClass().getCanonicalName(), f.getName()));
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
            LOGGER.trace(String.format("%s.%s is @OriginalFileName", f.getDeclaringClass().getCanonicalName(), f.getName()));
            String propertyName = fullyQualify(this.propertyName(f.getName()), this.getKeySeparator());
            if (StringUtils.hasLength(propertyName)) {
                ContentProperty property = properties.get(propertyName);
                if (property == null) {
                    property = new ContentProperty();
                    properties.put(propertyName, property);
                }
                updateContentProperty(property::setOriginalFileNamePropertyPath, fullyQualify(f.getName(), this.getContentPropertySeparator()));
            }
        } else if (f.getType().isPrimitive() == false &&
                   f.getType().equals(String.class) == false &&
                   f.getType().isEnum() == false &&
                   ContentPropertyUtils.isWrapperType(f.getType()) == false &&
                   ContentPropertyUtils.isRelationshipField(f) == false) {

            LOGGER.trace(String.format("%s.%s is subclass", f.getDeclaringClass().getCanonicalName(), f.getName()));

            ClassWalker subWalker = new ClassWalker(f.getType());
            ContentPropertyBuilderVisitor visitor = new SubClassVisitor(
                    this.keySeparator,
                    this.contentPropertySeparator,
                    this.nameProvider,
                    this instanceof SubClassVisitor ? (String[])ArrayUtils.add(((SubClassVisitor)this).propertyPathPrefix, f.getName()) : new String[] {f.getName()},
                    this.getClassesVisited());

            subWalker.accept(visitor);
            for (Entry<String, ContentProperty> entry : visitor.getProperties().entrySet()) {
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

        public SubClassVisitor(CharSequence keySeparator, CharSequence contentPropertySeparator, NameProvider nameProvider, String[] propertyPathPrefix, List<Class<?>> classesVisited) {
            super(keySeparator, contentPropertySeparator, nameProvider, classesVisited);
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
