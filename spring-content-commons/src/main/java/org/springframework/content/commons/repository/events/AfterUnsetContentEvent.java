package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterUnsetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = 3984922393423249069L;

	public AfterUnsetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
