package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class AfterSetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -4974444274997145136L;

	public AfterSetContentEvent(Object source) {
		super(source);
	}

}
