package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterUnsetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = 3984922393423249069L;

	public AfterUnsetContentEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
	}
}
