package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.AfterSetContentEvent} instead.
 */
public class AfterSetContentEvent extends AfterStoreEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	public AfterSetContentEvent(Object source, Store<Serializable> store) {
		super(source, store);
	}

    public AfterSetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
