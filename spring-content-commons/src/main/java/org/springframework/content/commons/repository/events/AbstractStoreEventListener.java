package org.springframework.content.commons.repository.events;

import static org.springframework.core.GenericTypeResolver.resolveTypeArgument;

import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.context.ApplicationListener;

public abstract class AbstractStoreEventListener<T>
		implements ApplicationListener<StoreEvent> {

	private final Class<?> INTERESTED_TYPE = resolveTypeArgument(getClass(), AbstractStoreEventListener.class);

	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void onApplicationEvent(StoreEvent event) {

		Class<?> srcType = event.getSource().getClass();

		if (null != INTERESTED_TYPE && !INTERESTED_TYPE.isAssignableFrom(srcType)) {
			return;
		}

		if (event instanceof BeforeGetContentEvent) {
			onBeforeGetContent((BeforeGetContentEvent) event);
			onBeforeGetContent((T) event.getSource());
		} else if (event instanceof AfterGetContentEvent) {
			onAfterGetContent((AfterGetContentEvent) event);
			onAfterGetContent((T) event.getSource());
		} else if (event instanceof BeforeSetContentEvent) {
			onBeforeSetContent((BeforeSetContentEvent) event);
			onBeforeSetContent((T) event.getSource());
		} else if (event instanceof AfterSetContentEvent) {
			onAfterSetContent((AfterSetContentEvent) event);
			onAfterSetContent((T) event.getSource());
		} else if (event instanceof BeforeUnsetContentEvent) {
			onBeforeUnsetContent((BeforeUnsetContentEvent) event);
			onBeforeUnsetContent((T) event.getSource());
		} else if (event instanceof AfterUnsetContentEvent) {
			onAfterUnsetContent((AfterUnsetContentEvent) event);
			onAfterUnsetContent((T) event.getSource());
		}
	}
	
	/**
	 * Override this method if you are interested in {@literal beforeGetContent} events.
	 * 
	 * @param event The event
	 */
	protected void onBeforeGetContent(BeforeGetContentEvent event) {}

	/**
	 * Override this method if you are interested in {@literal beforeGetContent} events.
	 * 
	 * @param entity The content entity being fetched.
	 */
	protected void onBeforeGetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterGetContent} events.
	 * 
	 * @param event The event
	 */
	protected void onAfterGetContent(AfterGetContentEvent event) {}

	/**
	 * Override this method if you are interested in {@literal afterGetContent} events.
	 * 
	 * @param entity The content entity being fetched.
	 */
	protected void onAfterGetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeSetContent} events.
	 * 
	 * @param entity The content event
	 */
	protected void onBeforeSetContent(BeforeSetContentEvent entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeSetContent} events.
	 * 
	 * @param entity The content entity being updated.
	 */
	protected void onBeforeSetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterSetContent} events.
	 * 
	 * @param event The content event
	 */
	protected void onAfterSetContent(AfterSetContentEvent event) {}

	/**
	 * Override this method if you are interested in {@literal afterSetContent} events.
	 * 
	 * @param entity The content entity being updated.
	 */
	protected void onAfterSetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeUnsetContent} events.
	 * 
	 * @param event The content event
	 */
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {}

	/**
	 * Override this method if you are interested in {@literal beforeUnsetContent} events.
	 * 
	 * @param entity The content entity being removed.
	 */
	protected void onBeforeUnsetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterUnsetContent} events.
	 * 
	 * @param event The content event
	 */
	protected void onAfterUnsetContent(AfterUnsetContentEvent event) {}

	/**
	 * Override this method if you are interested in {@literal afterUnsetContent} events.
	 * 
	 * @param entity The content entity being removed.
	 */
	protected void onAfterUnsetContent(T entity) {}
}
