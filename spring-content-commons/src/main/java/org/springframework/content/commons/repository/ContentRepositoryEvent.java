package org.springframework.content.commons.repository;

import org.springframework.context.ApplicationEvent;

public class ContentRepositoryEvent extends ApplicationEvent {
	private static final long serialVersionUID = -4985896308323075130L;

	public ContentRepositoryEvent(Object source) {
		super(source);
	}
}
