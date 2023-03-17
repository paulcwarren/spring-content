package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

public class StoreEvent extends org.springframework.content.commons.repository.StoreEvent {
	private static final long serialVersionUID = -4985896308323075130L;

	private ContentStore<Object, Serializable> store = null;
    private PropertyPath propertyPath;

	public StoreEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
		this.store = store;
	}

    public StoreEvent(Object source, PropertyPath properyPath, ContentStore<Object, Serializable> store) {
        super(source, properyPath, store);
        this.propertyPath = properyPath;
        this.store = store;
    }
}
