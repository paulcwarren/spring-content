package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class BeforeGetContentEvent extends StoreEvent {

	private static final long serialVersionUID = -6943798939368100773L;

	public BeforeGetContentEvent(Object source, ContentStore<Object,Serializable> store) {
		super(source, store);
	}

}
