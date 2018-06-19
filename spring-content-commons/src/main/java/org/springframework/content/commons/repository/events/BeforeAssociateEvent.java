package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;

import java.io.Serializable;

public class BeforeAssociateEvent extends StoreEvent {
    public BeforeAssociateEvent(Object source, ContentStore<Object, Serializable> store) {
        super(source, store);
    }
}
