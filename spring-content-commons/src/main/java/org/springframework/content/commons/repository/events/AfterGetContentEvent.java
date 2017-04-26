package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterGetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -3209578443616933734L;

	public AfterGetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}
}
