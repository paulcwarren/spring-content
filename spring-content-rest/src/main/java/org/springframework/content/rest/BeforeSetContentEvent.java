package org.springframework.content.rest;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import lombok.Getter;

@Getter
public class BeforeSetContentEvent extends StoreRestEvent {

	private static final long serialVersionUID = -7299354365313770L;

	private InputStream is;
	private Resource resource;

	public BeforeSetContentEvent(Object source, PropertyPath path, ContentStore<Object, Serializable> store, Resource resource, MediaType resourceType) {
        super(source, path, store, resource, resourceType);
	}
}
