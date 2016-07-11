package org.springframework.content.common.utils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

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
		return false;
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
