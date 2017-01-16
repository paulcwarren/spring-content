package org.springframework.content.commons.repository.events;

import static org.springframework.core.GenericTypeResolver.resolveTypeArgument;

import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.context.ApplicationListener;

public abstract class AbstractContentRepositoryEventListener<T>
		implements ApplicationListener<ContentRepositoryEvent> {

	private final Class<?> INTERESTED_TYPE = resolveTypeArgument(getClass(), AbstractContentRepositoryEventListener.class);

	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void onApplicationEvent(ContentRepositoryEvent event) {

		Class<?> srcType = event.getSource().getClass();

		if (null != INTERESTED_TYPE && !INTERESTED_TYPE.isAssignableFrom(srcType)) {
			return;
		}

		if (event instanceof BeforeGetContentEvent) {
				onBeforeGetContent((T) event.getSource());
		} else if (event instanceof AfterGetContentEvent) {
			onAfterGetContent((T) event.getSource());
		} else if (event instanceof BeforeSetContentEvent) {
			onBeforeSetContent((T) event.getSource());
		} else if (event instanceof AfterSetContentEvent) {
			onAfterSetContent((T) event.getSource());
		} else if (event instanceof BeforeUnsetContentEvent) {
			onBeforeUnsetContent((T) event.getSource());
		} else if (event instanceof AfterUnsetContentEvent) {
			onAfterUnsetContent((T) event.getSource());
		}
	}
	
	/**
	 * Override this method if you are interested in {@literal beforeGetContent} events.
	 * 
	 * @param entity The content entity being fetched.
	 */
	protected void onBeforeGetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterGetContent} events.
	 * 
	 * @param entity The content entity being fetched.
	 */
	protected void onAfterGetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeSetContent} events.
	 * 
	 * @param entity The content entity being updated.
	 */
	protected void onBeforeSetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterSetContent} events.
	 * 
	 * @param entity The content entity being updated.
	 */
	protected void onAfterSetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeUnsetContent} events.
	 * 
	 * @param entity The content entity being removed.
	 */
	protected void onBeforeUnsetContent(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterUnsetContent} events.
	 * 
	 * @param entity The content entity being removed.
	 */
	protected void onAfterUnsetContent(T entity) {}
}
