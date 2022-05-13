package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class BeforeAssociateEvent extends StoreRestEvent {

    public BeforeAssociateEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
