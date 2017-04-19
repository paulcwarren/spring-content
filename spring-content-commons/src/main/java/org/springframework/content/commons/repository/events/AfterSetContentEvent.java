package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterSetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	public AfterSetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
