package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.events.AfterStoreEvent} instead.
 */
public class AfterStoreEvent extends StoreEvent {

    private Object result;

    public AfterStoreEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public AfterStoreEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
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
