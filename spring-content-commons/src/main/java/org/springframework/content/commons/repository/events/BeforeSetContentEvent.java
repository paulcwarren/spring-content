package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class BeforeSetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -7299354365313770L;

	public BeforeSetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
