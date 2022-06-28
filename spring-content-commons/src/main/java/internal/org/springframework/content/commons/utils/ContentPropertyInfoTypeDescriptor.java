package internal.org.springframework.content.commons.utils;

import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Creates {@link TypeDescriptor}s for {@link ContentPropertyInfo} class
 * that include information about generic parameter types
 * (in contrast to {@code TypeDescriptor typeDescriptor = TypeDescriptor.forObject(contentPropertyInfo)}).
 */
public abstract class ContentPropertyInfoTypeDescriptor {

    public static TypeDescriptor withGenerics(Object entity, ContentProperty property) {
        ResolvableType contentPropertyInfoResolvableType = ResolvableType.forClassWithGenerics(ContentPropertyInfo.class,
                entity.getClass(), property.getContentIdType(entity).getObjectType());
        TypeDescriptor contentPropertyInfoType = new TypeDescriptor(contentPropertyInfoResolvableType, null,
                null);  // type annotations are not relevant for conversion of ContentPropertyInfo

        return contentPropertyInfoType;
    }

    public static TypeDescriptor withGenerics(Class<?> entityClass, Class<?> contentIdClass) {
        ResolvableType contentPropertyInfoResolvableType = ResolvableType.forClassWithGenerics(ContentPropertyInfo.class,
                entityClass, contentIdClass);
        TypeDescriptor contentPropertyInfoType = new TypeDescriptor(contentPropertyInfoResolvableType, null,
                null); // type annotations are not relevant for conversion of ContentPropertyInfo

        return contentPropertyInfoType;
    }
}
