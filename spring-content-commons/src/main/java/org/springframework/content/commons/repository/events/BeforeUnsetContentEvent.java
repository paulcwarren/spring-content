package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class BeforeUnsetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = 2662992853516955647L;

	public BeforeUnsetContentEvent(Object source) {
		super(source);
	}

}
