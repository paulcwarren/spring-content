package org.springframework.content.commons.repository;

import java.io.Serializable;

public class AfterStoreEvent extends StoreEvent {

    private Object result;

    public AfterStoreEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
