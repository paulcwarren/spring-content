package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

import java.io.Serializable;

public class BeforeGetResourceEvent extends StoreEvent {

    public BeforeGetResourceEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }
}
