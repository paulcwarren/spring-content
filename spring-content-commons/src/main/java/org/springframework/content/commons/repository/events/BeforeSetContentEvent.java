package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentStore;

public class BeforeSetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -7299354365313770L;

	public BeforeSetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
