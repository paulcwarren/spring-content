package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class BeforeGetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Object source) {
		super(source);
	}

}
