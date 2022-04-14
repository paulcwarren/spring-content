package org.springframework.content.rest;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterSetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	private Object source;
	private PropertyPath path;

	public AfterSetContentEvent(Object source, PropertyPath path, Resource resource, MediaType resourceType) {
        super(resource, resourceType);
        this.source = source;
        this.path = path;
	}

	public Object getActualSource() {
	    return source;
	}

	public PropertyPath getPropetyPath() {
	    return path;
	}
}
