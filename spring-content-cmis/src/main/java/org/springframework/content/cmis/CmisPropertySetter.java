package org.springframework.content.cmis;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class CmisPropertySetter {

	private final Properties properties;

	public CmisPropertySetter(Properties properties) {
		this.properties = properties;
	}

	public void populate(Object bean) {

		BeanWrapper wrapper = new BeanWrapperImpl(bean);

		Map<String, PropertyData<?>> props = properties.getProperties();
		for (String name : props.keySet()) {
			String strippedName = name.replace("cmis:", "");

			PropertyDescriptor descriptor = findCmisProperty(strippedName, wrapper);

			if (descriptor != null) {
				if (!isIndexedProperty(descriptor) && !isMapProperty(descriptor)) {
					wrapper.setPropertyValue(descriptor.getName(), props.get(name).getFirstValue());
				} else {
					wrapper.setPropertyValue(descriptor.getName(), props.get(name).getValues());
				}
			}
		}
	}

	boolean isIndexedProperty(PropertyDescriptor descriptor) {

		if (descriptor.getPropertyType().isAssignableFrom(Collection.class) ||
			descriptor.getPropertyType().isArray())
			return true;

		return false;
	}

	boolean isMapProperty(PropertyDescriptor descriptor) {

		if (descriptor.getPropertyType().isAssignableFrom(Map.class))
			return true;

		return false;
	}

	PropertyDescriptor findCmisProperty(String strippedName, BeanWrapper wrapper) {

		if (strippedName.equals("objectTypeId"))
			return null;

		PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : descriptors) { if (descriptor.getName().equals(strippedName))
				return descriptor;
		}

		return null;
	}
}
