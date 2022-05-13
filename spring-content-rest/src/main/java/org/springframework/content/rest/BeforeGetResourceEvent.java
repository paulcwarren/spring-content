package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class BeforeGetResourceEvent extends StoreRestEvent {

    private static final long serialVersionUID = -4288863659935527531L;

    public BeforeGetResourceEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
    }
}
