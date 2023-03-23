package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.store.Store;

import java.io.Serializable;

public class AfterGetResourceEvent extends AfterStoreEvent {
    public AfterGetResourceEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public AfterGetResourceEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
