package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public class AfterUnassociateEvent extends AfterStoreEvent {
    public AfterUnassociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterUnassociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
