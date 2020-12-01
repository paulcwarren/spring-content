package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.context.ApplicationEvent;

public class StoreEvent extends ApplicationEvent {
	private static final long serialVersionUID = -4985896308323075130L;

	private ContentStore<Object, Serializable> store = null;
    private PropertyPath propertyPath;

	public StoreEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source);
		this.store = store;
	}

    public StoreEvent(Object source, PropertyPath properyPath, ContentStore<Object, Serializable> store) {
        super(source);
        this.propertyPath = properyPath;
        this.store = store;
    }

    public PropertyPath getPropertyPath() {
        return propertyPath;
    }

	public ContentStore<Object, Serializable> getStore() {
		return store;
	}
}
