package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterUnassociateEvent extends AfterStoreEvent {

    private static final long serialVersionUID = -1981687210695835698L;

    public AfterUnassociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterUnassociateEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
