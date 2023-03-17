package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.BeforeUnsetContentEvent} instead.
 */
public class BeforeUnsetContentEvent extends StoreEvent {

	private static final long serialVersionUID = 2662992853516955647L;

	public BeforeUnsetContentEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
	}

    public BeforeUnsetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
