package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

public class StoreEvent extends org.springframework.content.commons.repository.StoreEvent {
	private static final long serialVersionUID = -4985896308323075130L;

	private Store<Serializable> store = null;
    private PropertyPath propertyPath;

	public StoreEvent(Object source, Store<Serializable> store) {
		super(source, store);
		this.store = store;
	}

    public StoreEvent(Object source, PropertyPath properyPath, Store<Serializable> store) {
        super(source, properyPath, store);
        this.propertyPath = properyPath;
        this.store = store;
    }
}
