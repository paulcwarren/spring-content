package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreEvent;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.BeforeGetContentEvent} instead.
 */
public class BeforeGetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Object source, Store<Serializable> store) {
		super(source, store);
	}

    public BeforeGetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
