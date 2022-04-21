package org.springframework.content.rest;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterSetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	public AfterSetContentEvent(Object source, PropertyPath path, ContentStore<Object, Serializable> store, Resource resource, MediaType resourceType) {
        super(source, path, store, resource, resourceType);
	}
}
