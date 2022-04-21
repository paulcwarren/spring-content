package org.springframework.content.rest;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class BeforeGetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Object source, PropertyPath path, ContentStore<Object, Serializable> store, Resource resource, MediaType resourceType) {
        super(source, path, store, resource, resourceType);
	}
}
