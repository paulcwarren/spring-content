package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class AfterUnsetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = 3984922393423249069L;

	public AfterUnsetContentEvent(Object source) {
		super(source);
	}

}
