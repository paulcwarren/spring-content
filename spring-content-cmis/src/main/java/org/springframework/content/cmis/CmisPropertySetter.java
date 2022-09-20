package org.springframework.content.cmis;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.utils.BeanUtils;

public class CmisPropertySetter {

	private final Properties properties;

	public CmisPropertySetter(Properties properties) {
		this.properties = properties;
	}

	public void populate(Object bean, PropertyData... additionalProperties) {
		if (properties == null) {
			return;
		}

		BeanWrapper wrapper = new BeanWrapperImpl(bean);

		Map<String, PropertyData<?>> props = properties.getProperties();
		for (String name : props.keySet()) {

			if ("cmis:objectTypeId".equals(name)) {
				continue;
			}

			Field[] fields = null;
			switch (name) {
				case "cmis:name":
					setCmisProperty(CmisName.class, wrapper, props.get(name).getValues());
					break;
				case "cmis:description":
					setCmisProperty(CmisDescription.class, wrapper, props.get(name).getValues());
					break;
			}
		}

		for (PropertyData property : additionalProperties) {
			switch(property.getId()) {
				case "cmis:contentStreamFileName":
					setCmisProperty(CmisFileName.class, wrapper, property.getValues());
					break;
			}
		}
	}

	void setCmisProperty(Class<? extends Annotation> cmisAnnotationClass, BeanWrapper wrapper, List<?> values) {
		Field[] fields = findCmisProperty(cmisAnnotationClass, wrapper);

		if (fields != null) {
			for (Field field : fields) {
				if (!isIndexedProperty(field) && !isMapProperty(field)) {
					wrapper.setPropertyValue(field.getName(), (values.size() >= 1) ? values.get(0) : null);
				} else {
					wrapper.setPropertyValue(field.getName(), values);
				}
			}
		}
	}

	boolean isIndexedProperty(Field field) {

		if (field.getType().isAssignableFrom(Collection.class) ||
				field.getType().isArray())
			return true;

		return false;
	}

	boolean isMapProperty(Field field) {

		if (field.getType().isAssignableFrom(Map.class))
			return true;

		return false;
	}

	Field[] findCmisProperty(Class<? extends Annotation> annotation, BeanWrapper wrapper) {
		return BeanUtils.findFieldsWithAnnotation(wrapper.getWrappedClass(), annotation, wrapper);
	}
}
