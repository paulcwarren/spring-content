package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public class AfterStoreEvent extends StoreEvent {

    private Object result;

    public AfterStoreEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public AfterStoreEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }

    @Deprecated
    public void setResult(Object result) {
        this.result = result;
    }

    @Deprecated
    public Object getResult() {
        return result;
    }
}
