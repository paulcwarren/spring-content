package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.store.Store;

import java.io.Serializable;

public class BeforeUnassociateEvent extends StoreEvent {
    public BeforeUnassociateEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeUnassociateEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
