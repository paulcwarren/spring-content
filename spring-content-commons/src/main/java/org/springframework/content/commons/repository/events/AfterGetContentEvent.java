package org.springframework.content.commons.repository.events;

import org.springframework.content.commons.repository.ContentRepositoryEvent;

public class AfterGetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -3209578443616933734L;

	public AfterGetContentEvent(Object source) {
		super(source);
	}
}
