package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;

import java.io.Serializable;

public class AfterSetContentEvent extends AfterStoreEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	public AfterSetContentEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
	}

}
