package internal.org.springframework.content.commons.repository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.content.commons.annotations.HandleAfterAssociate;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterGetResource;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnassociate;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeAssociate;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetResource;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnassociate;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.events.AfterAssociateEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnassociateEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeAssociateEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnassociateEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

public class AnnotatedStoreEventInvoker
		implements ApplicationListener<StoreEvent>, BeanPostProcessor {

	private static final Log logger = LogFactory.getLog(AnnotatedStoreEventInvoker.class);

	private final MultiValueMap<Class<? extends StoreEvent>, EventHandlerMethod> handlerMethods = new LinkedMultiValueMap<Class<? extends StoreEvent>, EventHandlerMethod>();

	private ReflectionService reflectionService;

	public AnnotatedStoreEventInvoker() {
		reflectionService = new ReflectionServiceImpl();
	}

	public AnnotatedStoreEventInvoker(ReflectionService reflectionService) {
		this.reflectionService = reflectionService;
	}

	MultiValueMap<Class<? extends StoreEvent>, EventHandlerMethod> getHandlers() {
		return handlerMethods;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		Class<?> beanType = ClassUtils.getUserClass(bean);
		StoreEventHandler typeAnno = AnnotationUtils.findAnnotation(beanType,
				StoreEventHandler.class);

		if (typeAnno == null) {
			return bean;
		}

		ReflectionUtils.doWithMethods(beanType, new ReflectionUtils.MethodCallback() {

			@Override
			public void doWith(Method method)
					throws IllegalArgumentException, IllegalAccessException {
				findHandler(bean, method, HandleBeforeGetResource.class,
						BeforeGetResourceEvent.class);
				findHandler(bean, method, HandleAfterGetResource.class,
						AfterGetResourceEvent.class);
				findHandler(bean, method, HandleBeforeAssociate.class,
						BeforeAssociateEvent.class);
				findHandler(bean, method, HandleAfterAssociate.class,
						AfterAssociateEvent.class);
				findHandler(bean, method, HandleBeforeUnassociate.class,
						BeforeUnassociateEvent.class);
				findHandler(bean, method, HandleAfterUnassociate.class,
						AfterUnassociateEvent.class);
				findHandler(bean, method, HandleBeforeGetContent.class,
						BeforeGetContentEvent.class);
				findHandler(bean, method, HandleAfterGetContent.class,
						AfterGetContentEvent.class);
				findHandler(bean, method, HandleBeforeSetContent.class,
						BeforeSetContentEvent.class);
				findHandler(bean, method, HandleAfterSetContent.class,
						AfterSetContentEvent.class);
				findHandler(bean, method, HandleBeforeUnsetContent.class,
						BeforeUnsetContentEvent.class);
				findHandler(bean, method, HandleAfterUnsetContent.class,
						AfterUnsetContentEvent.class);
			}

		});

		return bean;
	}

	@Override
	public void onApplicationEvent(StoreEvent event) {
		Class<? extends StoreEvent> eventType = event.getClass();

		if (!handlerMethods.containsKey(eventType)) {
			return;
		}

		for (EventHandlerMethod handlerMethod : handlerMethods.get(eventType)) {

			Object src = event.getSource();

			if ((ClassUtils.isAssignable(StoreEvent.class, handlerMethod.targetType) &&
					ClassUtils.isAssignable(handlerMethod.targetType, event.getClass()) == false) ||
					(ClassUtils.isAssignable(StoreEvent.class, handlerMethod.targetType) == false &&
							ClassUtils.isAssignable(handlerMethod.targetType, src.getClass()) == false)) {
				continue;
			}

			List<Object> parameters = new ArrayList<Object>();
			if (ClassUtils.isAssignable(StoreEvent.class, handlerMethod.targetType)) {
				parameters.add(event);
			} else {
				parameters.add(src);
			}

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Invoking {} handler for {}.",
						event.getClass().getSimpleName(), event.getSource()));
			}

			reflectionService.invokeMethod(handlerMethod.method, handlerMethod.handler,
					parameters.toArray());
		}
	}

	<H extends Annotation, E> void findHandler(Object bean, Method method,
			Class<H> handler, Class<? extends StoreEvent> eventType) {
		H annotation = AnnotationUtils.findAnnotation(method, handler);

		if (annotation == null) {
			return;
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		if (parameterTypes.length == 0) {
			throw new IllegalStateException(String.format(
					"Event handler method %s must have a content object argument",
					method.getName()));
		}

		EventHandlerMethod handlerMethod = new EventHandlerMethod(parameterTypes[0], bean,
				method);

		logger.debug(
				String.format("Annotated handler method found: {%s}", handlerMethod));

		List<EventHandlerMethod> events = handlerMethods.get(eventType);

		if (events == null) {
			events = new ArrayList<>();
		}

		if (events.isEmpty()) {
			handlerMethods.add(eventType, handlerMethod);
			return;
		}

		events.add(handlerMethod);
		Collections.sort(events);
		handlerMethods.put(eventType, events);
	}

	static class EventHandlerMethod implements Comparable<EventHandlerMethod> {

		final Class<?> targetType;
		final Method method;
		final Object handler;

		private EventHandlerMethod(Class<?> targetType, Object handler, Method method) {

			this.targetType = targetType;
			this.method = method;
			this.handler = handler;

			ReflectionUtils.makeAccessible(this.method);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(EventHandlerMethod o) {
			return AnnotationAwareOrderComparator.INSTANCE.compare(this.method, o.method);
		}

		@Override
		public String toString() {
			return String.format(
					"EventHandlerMethod{ targetType=%s, method=%s, handler=%s }",
					targetType, method, handler);
		}
	}
}
