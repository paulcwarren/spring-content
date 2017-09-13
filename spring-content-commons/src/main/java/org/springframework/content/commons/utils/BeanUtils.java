package org.springframework.content.commons.utils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

public final class BeanUtils {

	private BeanUtils() {}
	
	public static boolean hasFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {

		BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			String prop = descriptor.getName();
			try {
				Field candidate = domainObj.getClass().getDeclaredField(prop);
				if (candidate != null) {
					if (candidate.getAnnotation(annotationClass) != null) {
						return true;
					}
				}
			} catch (NoSuchFieldException ex) {
				continue;
			}
		}
		
		for (Field field : domainObj.getClass().getFields()) {
			if (field.getAnnotation(annotationClass) != null) {
				return true;
			}
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
		BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			String prop = descriptor.getName();
			Field theField = null;
			try {
				theField = domainObj.getClass().getDeclaredField(prop);
				if (theField != null && theField.getAnnotation(annotationClass) != null) {
					type = theField.getType();
					break;
				}
			} catch (NoSuchFieldException ex) {
				continue;
			}
		}

		if (type == null) {
			for (Field field : domainObj.getClass().getFields()) {
				if (field.getAnnotation(annotationClass) != null) {
					try {
						type = field.getType();
					} catch (IllegalArgumentException iae) {}
				}
			}
		}
		
		return type;
	}

	public static Object getFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
			throws SecurityException, BeansException {
		Object value = null;
		BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			String prop = descriptor.getName();
			Field theField = null;
			try {
				theField = domainObj.getClass().getDeclaredField(prop);
				if (theField != null) {
					if (theField.getAnnotation(annotationClass) != null) {
						value = wrapper.getPropertyValue(prop);
					}
				}
			} catch (NoSuchFieldException ex) {
				continue;
			}
		}

		if (value == null) {
			for (Field field : domainObj.getClass().getFields()) {
				if (field.getAnnotation(annotationClass) != null) {
					try {
						value = ReflectionUtils.getField(field, domainObj);
					} catch (IllegalArgumentException iae) {}
				}
			}
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

		BeanWrapper wrapper = new BeanWrapperImpl(domainObj);

		for (Field field : domainObj.getClass().getFields()) {
			if (field.getAnnotation(annotationClass) != null) {
				try {
					ReflectionUtils.setField(field, domainObj, value);
				} catch (IllegalArgumentException iae) {}
			}
		}

		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) {
			String prop = descriptor.getName();
			Field theField = null;
			try {
				theField = domainObj.getClass().getDeclaredField(prop);
				if (theField != null) {
					if (theField.getAnnotation(annotationClass) != null) {
						wrapper.setPropertyValue(prop, value);
					}
				}
			} catch (NoSuchFieldException ex) {
				continue;
			}
		}
	}
}
