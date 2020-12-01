package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

public class BeforeAssociateEvent extends StoreEvent {

    public BeforeAssociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public BeforeAssociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
