package org.springframework.content.commons.utils;

import java.lang.reflect.Field;

public final class DomainObjectUtils {

    private static boolean JAVAX_PERSISTENCE_ID_CLASS_PRESENT = false;

    static {
        try {
            JAVAX_PERSISTENCE_ID_CLASS_PRESENT = DomainObjectUtils.class.getClassLoader().loadClass("javax.persistence.Id") != null;
        } catch (ClassNotFoundException e) {}
    }

    private DomainObjectUtils() {}

    public static final Object getId(Object entity) {

        if (JAVAX_PERSISTENCE_ID_CLASS_PRESENT && BeanUtils.hasFieldWithAnnotation(entity, javax.persistence.Id.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, javax.persistence.Id.class);
        } else if (BeanUtils.hasFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }

        return null;
    }

    public static final Field getIdField(Class<?> domainClass) {

        if (JAVAX_PERSISTENCE_ID_CLASS_PRESENT && BeanUtils.findFieldWithAnnotation(domainClass, javax.persistence.Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, javax.persistence.Id.class);
        } else if (BeanUtils.findFieldWithAnnotation(domainClass, org.springframework.data.annotation.Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, org.springframework.data.annotation.Id.class);
        }

        return null;
    }
}
