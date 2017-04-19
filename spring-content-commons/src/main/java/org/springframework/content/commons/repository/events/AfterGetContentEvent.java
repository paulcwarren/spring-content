package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterGetContentEvent extends ContentRepositoryEvent {

	private static final long serialVersionUID = -3209578443616933734L;

	public AfterGetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}
}
