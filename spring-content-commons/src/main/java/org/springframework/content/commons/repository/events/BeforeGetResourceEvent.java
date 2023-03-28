package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreEvent;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.BeforeGetResourceEvent} instead.
 */
public class BeforeGetResourceEvent extends StoreEvent {

    private static final long serialVersionUID = -4288863659935527531L;

    public BeforeGetResourceEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeGetResourceEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
