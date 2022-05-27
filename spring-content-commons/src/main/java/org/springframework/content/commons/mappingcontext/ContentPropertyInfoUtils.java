package org.springframework.content.commons.mappingcontext;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;

public abstract class ContentPropertyInfoUtils {

    // Alternative to
    //  ContentPropertyInfo<S,SID> contentPropertyInfo = ...
    //  TypeDescriptor typeDescriptor = TypeDescriptor.forObject(contentPropertyInfo)
    // that captures generic parameter types.
    public static TypeDescriptor getTypeWithGenericParameters(Object entity, ContentProperty property) {


        ResolvableType contentPropertyInfoResolvableType = ResolvableType.forClassWithGenerics(ContentPropertyInfo.class, entity.getClass(),
                property.getContentIdType(entity).getObjectType());

        // maybe cache TypeDescriptor object as it is used frequently?
        TypeDescriptor contentPropertyInfoType = new TypeDescriptor(contentPropertyInfoResolvableType, null,
                null);  // type annotations are not relevant for conversion of ContentPropertyInfo

        return contentPropertyInfoType;
    }

    public static TypeDescriptor getTypeWithGenericParameters(Class<?> entityClass, Class<?> contentIdClass) {
        ResolvableType contentPropertyInfoResolvableType = ResolvableType.forClassWithGenerics(ContentPropertyInfo.class, entityClass,
                contentIdClass);

        // maybe cache TypeDescriptor object as it is used frequently?
        TypeDescriptor contentPropertyInfoType = new TypeDescriptor(contentPropertyInfoResolvableType, null,
                null); // type annotations are not relevant for conversion of ContentPropertyInfo

        return contentPropertyInfoType;
    }
}
