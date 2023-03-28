package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.context.ApplicationEvent;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.StoreEvent} instead.
 */
public class StoreEvent extends ApplicationEvent {
	private static final long serialVersionUID = -4985896308323075130L;

	private Store<Serializable> store = null;
    private PropertyPath propertyPath;

	public StoreEvent(Object source, Store<Serializable> store) {
		super(source);
		this.store = store;
	}

    public StoreEvent(Object source, PropertyPath properyPath, Store<Serializable> store) {
        super(source);
        this.propertyPath = properyPath;
        this.store = store;
    }

    public PropertyPath getPropertyPath() {
        return propertyPath;
    }

	public Store<Serializable> getStore() {
		return store;
	}
}
