package internal.org.springframework.content.commons.repository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.content.commons.annotations.ContentRepositoryEventHandler;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.repository.events.HandleBeforeUnsetContent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

public class AnnotatedContentRepositoryEventHandler
		implements ApplicationListener<ContentRepositoryEvent>, BeanPostProcessor {

	private static final Log logger = LogFactory.getLog(AnnotatedContentRepositoryEventHandler.class);

	private final MultiValueMap<Class<? extends ContentRepositoryEvent>, EventHandlerMethod> handlerMethods = new LinkedMultiValueMap<Class<? extends ContentRepositoryEvent>, EventHandlerMethod>();

	MultiValueMap<Class<? extends ContentRepositoryEvent>, EventHandlerMethod> getHandlers() {
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
		ContentRepositoryEventHandler typeAnno = AnnotationUtils.findAnnotation(beanType, ContentRepositoryEventHandler.class);

		if (typeAnno == null) {
			return bean;
		}
		
		ReflectionUtils.doWithMethods(beanType, new ReflectionUtils.MethodCallback() {

			@Override
			public void doWith(Method method)
					throws IllegalArgumentException, IllegalAccessException {
				findHandler(bean, method, HandleBeforeGetContent.class, BeforeGetContentEvent.class);
				findHandler(bean, method, HandleAfterGetContent.class, AfterGetContentEvent.class);
				findHandler(bean, method, HandleBeforeSetContent.class, BeforeSetContentEvent.class);
				findHandler(bean, method, HandleAfterSetContent.class, AfterSetContentEvent.class);
				findHandler(bean, method, HandleBeforeUnsetContent.class, BeforeUnsetContentEvent.class);
				findHandler(bean, method, HandleAfterUnsetContent.class, AfterUnsetContentEvent.class);
			}
			
		});

		return bean;
	}

	@Override
	public void onApplicationEvent(ContentRepositoryEvent event) {
	}
	
	<H extends Annotation, E> void findHandler(Object bean, Method method, Class<H> handler, Class<? extends ContentRepositoryEvent>  eventType) {
		H annotation = AnnotationUtils.findAnnotation(method, handler);

		if (annotation == null) {
			return;
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		if (parameterTypes.length == 0) {
			throw new IllegalStateException(String.format("Event handler method %s must have a content object argument", method.getName()));
		}

		EventHandlerMethod handlerMethod = new EventHandlerMethod(parameterTypes[0], bean, method);

		logger.debug(String.format("Annotated handler method found: {%s}", handlerMethod));

		handlerMethods.add(eventType, handlerMethod);
	}

	static class EventHandlerMethod {

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
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("EventHandlerMethod{ targetType=%s, method=%s, handler=%s }", targetType, method, handler);
		}
	}
}
