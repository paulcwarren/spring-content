package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class BeforeSetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -7299354365313770L;

	public BeforeSetContentEvent(Object source) {
		super(source);
	}

}
