package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterUnassociateEvent extends StoreRestEvent {

    private static final long serialVersionUID = -1981687210695835698L;

    public AfterUnassociateEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
