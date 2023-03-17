package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public class AfterAssociateEvent extends AfterStoreEvent {
    public AfterAssociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterAssociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
