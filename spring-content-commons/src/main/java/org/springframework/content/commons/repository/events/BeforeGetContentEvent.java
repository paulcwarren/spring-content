package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

public class BeforeGetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
	}

    public BeforeGetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
