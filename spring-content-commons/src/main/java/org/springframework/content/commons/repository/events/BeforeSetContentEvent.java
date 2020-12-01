package org.springframework.content.commons.repository.events;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.core.io.Resource;

import lombok.Getter;

@Getter
public class BeforeSetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -7299354365313770L;

	private InputStream is;
	private Resource resource;

	public BeforeSetContentEvent(Object source, ContentStore<Object, Serializable> store, InputStream is) {
		super(source, store);
		this.is = is;
	}

	public BeforeSetContentEvent(Object source, ContentStore<Object, Serializable> store, Resource resource) {
		super(source, store);
		this.resource = resource;
	}

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store, InputStream is) {
        super(source, propertyPath, store);
        this.is = is;
    }

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store, Resource resource) {
        super(source, propertyPath, store);
        this.resource = resource;
    }
}
