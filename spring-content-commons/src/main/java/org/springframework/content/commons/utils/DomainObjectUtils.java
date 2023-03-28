package org.springframework.content.commons.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;

public final class DomainObjectUtils {

    private static boolean JAVAX_PERSISTENCE_ID_CLASS_PRESENT = false;

    static {
        try {
            JAVAX_PERSISTENCE_ID_CLASS_PRESENT = DomainObjectUtils.class.getClassLoader().loadClass("jakarta.persistence.Id") != null;
        } catch (ClassNotFoundException e) {}
    }

    private DomainObjectUtils() {}

    public static final Object getId(Object entity) {

        Object id = null;

        if (JAVAX_PERSISTENCE_ID_CLASS_PRESENT && BeanUtils.hasFieldWithAnnotation(entity, jakarta.persistence.Id.class)) {

            id = BeanUtils.getFieldWithAnnotation(entity, jakarta.persistence.Id.class);
            if (id == null) {
                PropertyDescriptor[] propertyDescriptors = new BeanWrapperImpl(entity).getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (AnnotationUtils.findAnnotation(propertyDescriptor.getReadMethod(), jakarta.persistence.Id.class) != null) {
                        return new BeanWrapperImpl(entity).getPropertyValue(propertyDescriptor.getName());
                    }
                }
            }
        } else if (BeanUtils.hasFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class)) {
            id = BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
            if (id == null) {
                PropertyDescriptor[] propertyDescriptors = new BeanWrapperImpl(entity).getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (AnnotationUtils.findAnnotation(propertyDescriptor.getReadMethod(), org.springframework.data.annotation.Id.class) != null) {
                        return new BeanWrapperImpl(entity).getPropertyValue(propertyDescriptor.getName());
                    }
                }
            }
        }

        return id;
    }

    public static final Field getIdField(Class<?> domainClass) {

        if (JAVAX_PERSISTENCE_ID_CLASS_PRESENT && BeanUtils.findFieldWithAnnotation(domainClass, jakarta.persistence.Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, jakarta.persistence.Id.class);
        } else if (BeanUtils.findFieldWithAnnotation(domainClass, org.springframework.data.annotation.Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, org.springframework.data.annotation.Id.class);
        }

        return null;
    }
}
