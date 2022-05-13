package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterGetResourceEvent extends StoreRestEvent {

    private static final long serialVersionUID = -52677793449429582L;

    public AfterGetResourceEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
