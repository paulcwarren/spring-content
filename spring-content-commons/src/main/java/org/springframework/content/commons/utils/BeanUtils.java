package org.springframework.content.commons.utils;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BeanUtils {

	private static final Condition MATCHING_CONDITION = new Condition() {
		@Override
		public boolean matches(Field field) {
			return true;
		}
	};

	private BeanUtils() {}
	
	public static boolean hasFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            return true;
        }

		return false;
	}

	public static Field findFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {
		return findFieldWithAnnotation(domainObj.getClass(), annotationClass);
	}

	public static Field findFieldWithAnnotation(Class<?> domainObjClass, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {

		BeanWrapper wrapper = new BeanWrapperImpl(domainObjClass);
		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			Field candidate = getField(domainObjClass, descriptor.getName());
			if (candidate != null) {
				if (candidate.getAnnotation(annotationClass) != null) {
					return candidate;
				}
			}
		}
		
		for (Field field : getAllFields(domainObjClass)) {
			if (field.getAnnotation(annotationClass) != null) {
				return field;
			}
		}
		
		return null;
	}
	
	protected static List<Field> getAllFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
	    fields.addAll(Arrays.asList(type.getDeclaredFields()));

	    if (type.getSuperclass() != null) {
	        getAllFields(fields, type.getSuperclass());
	    }

	    return fields;
	}

	protected static List<Field> getAllFields(List<Field> fields, Class<?> type) {
	    fields.addAll(Arrays.asList(type.getDeclaredFields()));

	    if (type.getSuperclass() != null) {
	        getAllFields(fields, type.getSuperclass());
	    }

	    return fields;
	}
	
	protected static Field getField(Class<?> type, String fieldName) {
		for (Field field : getAllFields(type)) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}
		return null;
	}

	public static Class<?> getFieldWithAnnotationType(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {
		Class<?> type = null;

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            type = field.getType();
        }

		return type;
	}

	public static Object getFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {
		Object value = null;

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            try {
                PropertyDescriptor descriptor = org.springframework.beans.BeanUtils.getPropertyDescriptor(domainObj.getClass(), field.getName());
                if (descriptor != null) {
                    BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
                    value = wrapper.getPropertyValue(field.getName());
                } else {
                    value = ReflectionUtils.getField(field, domainObj);
                }
                return value;
            } catch (IllegalArgumentException iae) {}
        }

		return value;
	}

	/**
	 * Sets object's field annotated with annotationClass to value.
	 *
	 * @param domainObj
	 * 					the object containing the field
	 * @param annotationClass
	 * 					the annotation to look for
	 * @param value
	 * 					the value to set
	 */
	public static void setFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass, Object value) {
		setFieldWithAnnotationConditionally(domainObj, annotationClass, value, MATCHING_CONDITION);
	}
	
	/**
	 * Sets object's field annotated with annotationClass to value only if the condition matches.
	 *
	 * @param domainObj
	 * 					the object containing the field
	 * @param annotationClass
	 * 					the annotation to look for
	 * @param value
	 * 					the value to set
	 * @param condition
	 * 					the condition that must be satisfied to allow the match
	 */
	public static void setFieldWithAnnotationConditionally(Object domainObj, Class<? extends Annotation> annotationClass, Object value, Condition condition) {

		Field field = findFieldWithAnnotation(domainObj, annotationClass);
		if (field != null && field.getAnnotation(annotationClass) != null && condition.matches(field)) {
			try {
			    PropertyDescriptor descriptor = org.springframework.beans.BeanUtils.getPropertyDescriptor(domainObj.getClass(), field.getName());
			    if (descriptor != null) {
                    BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
			        wrapper.setPropertyValue(field.getName(), value);
                    return;
                } else {
                    ReflectionUtils.setField(field, domainObj, value);
                }
				return;
			} catch (IllegalArgumentException iae) {}
		}
	}
}
