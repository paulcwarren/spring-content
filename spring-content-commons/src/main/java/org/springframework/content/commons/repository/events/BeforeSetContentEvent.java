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

	private InputStream inputStream;
	private Resource resource;

	public BeforeSetContentEvent(Object source, ContentStore<Object, Serializable> store, InputStream is) {
		super(source, store);
		this.inputStream = is;
	}

	public BeforeSetContentEvent(Object source, ContentStore<Object, Serializable> store, Resource resource) {
		super(source, store);
		this.resource = resource;
	}

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store, InputStream is) {
        super(source, propertyPath, store);
        this.inputStream = is;
    }

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store, Resource resource) {
        super(source, propertyPath, store);
        this.resource = resource;
    }

    /**
     * Deprecated.
     *
     * Use getInputStream instead
     *
     * @return the event's input stream
     */
    @Deprecated()
    public InputStream getIs() {
        return this.inputStream;
    }

    public void setInputStream(InputStream is) {
        this.inputStream = is;
    }
}
