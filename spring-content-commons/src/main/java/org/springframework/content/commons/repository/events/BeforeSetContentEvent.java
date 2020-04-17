package org.springframework.content.commons.repository.events;

import java.io.InputStream;
import java.io.Serializable;

import lombok.Getter;

import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;

@Getter
public class BeforeSetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -7299354365313770L;

	private final InputStream is;

	public BeforeSetContentEvent(Object source, ContentStore<Object, Serializable> store, InputStream is) {
		super(source, store);
		this.is = is;
	}
}
