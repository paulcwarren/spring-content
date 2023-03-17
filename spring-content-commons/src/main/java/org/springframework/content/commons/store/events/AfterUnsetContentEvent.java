package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public class AfterUnsetContentEvent extends AfterStoreEvent {
	public AfterUnsetContentEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
	}

	public AfterUnsetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
		super(source, propertyPath, store);
	}
}
