package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class BeforeGetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Resource resource, MediaType resourceType) {
		super(resource, resourceType);
	}
}
