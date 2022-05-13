package org.springframework.content.rest;

import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import lombok.Getter;

@Getter
public class BeforeSetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -7299354365313770L;

	private InputStream is;
	private Resource resource;

	public BeforeSetContentEvent(Resource resource, MediaType resourceType) {
        super(resource, resourceType);
	}
}
