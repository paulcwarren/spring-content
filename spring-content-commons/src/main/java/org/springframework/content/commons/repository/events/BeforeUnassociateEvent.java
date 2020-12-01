package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

public class BeforeUnassociateEvent extends StoreEvent {

    public BeforeUnassociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public BeforeUnassociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
