package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.AfterAssociateEvent} instead.
 */
public class AfterAssociateEvent extends AfterStoreEvent {

    private static final long serialVersionUID = -1256654776081821449L;

    public AfterAssociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterAssociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
