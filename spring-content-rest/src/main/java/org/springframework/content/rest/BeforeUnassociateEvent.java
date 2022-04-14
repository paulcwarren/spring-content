package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class BeforeUnassociateEvent extends StoreRestEvent {

    public BeforeUnassociateEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
