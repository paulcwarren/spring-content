package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentStore;

public class BeforeUnsetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = 2662992853516955647L;

	public BeforeUnsetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
