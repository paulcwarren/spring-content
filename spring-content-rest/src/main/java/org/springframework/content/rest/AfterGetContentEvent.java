package org.springframework.content.rest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class AfterGetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -3209578443616933734L;

	public AfterGetContentEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
	}
}
