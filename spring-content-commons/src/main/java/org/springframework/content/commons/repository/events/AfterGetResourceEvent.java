package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterGetResourceEvent extends AfterStoreEvent {

    private static final long serialVersionUID = -52677793449429582L;

    public AfterGetResourceEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterGetResourceEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
