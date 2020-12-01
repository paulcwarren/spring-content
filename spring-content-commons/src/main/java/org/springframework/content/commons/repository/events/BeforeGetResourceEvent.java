package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

public class BeforeGetResourceEvent extends StoreEvent {

    private static final long serialVersionUID = -4288863659935527531L;

    public BeforeGetResourceEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public BeforeGetResourceEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
