package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterAssociateEvent extends StoreRestEvent {

    private static final long serialVersionUID = -1256654776081821449L;

    public AfterAssociateEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
