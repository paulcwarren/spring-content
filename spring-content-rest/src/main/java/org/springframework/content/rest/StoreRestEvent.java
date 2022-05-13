package org.springframework.content.rest;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class StoreRestEvent extends ApplicationEvent {

    private Resource resource;
    private MediaType resourceType;

	public StoreRestEvent(Resource resource, MediaType resourceType) {
		super(resource);
		this.resource = resource;
		this.resourceType = resourceType;
	}

    public Resource getResource() {
        return resource;
    }

    public MediaType getResourceType() {
        return resourceType;
    }
}
