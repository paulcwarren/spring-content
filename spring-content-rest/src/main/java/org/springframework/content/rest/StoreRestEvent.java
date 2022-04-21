package org.springframework.content.rest;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class StoreRestEvent extends StoreEvent {

    private static final long serialVersionUID = -7969556410287283948L;

    private Resource resource;
    private MediaType resourceType;

	public StoreRestEvent(Object source, PropertyPath properyPath, ContentStore<Object, Serializable> store, Resource resource, MediaType resourceType) {
		super(source, properyPath, store);
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
