package internal.org.springframework.content.commons.repository.factory;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.SearchException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import internal.org.springframework.content.commons.repository.ContentRepositoryInvokerImpl;

public class ContentRepositoryMethodInteceptor implements MethodInterceptor {

	private Map<Method,ContentRepositoryExtension> extensions;
	private ApplicationEventPublisher publisher;
	
	private static Method getContentMethod; 
	private static Method setContentMethod; 
	private static Method unsetContentMethod;
    private Class<?> domainClass = null;
    private Class<? extends Serializable> contentIdClass = null;
	
	static {
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
		Assert.notNull(getContentMethod);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
		Assert.notNull(setContentMethod);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
		Assert.notNull(unsetContentMethod);
	}
	
	public ContentRepositoryMethodInteceptor(Class<?> domainClass, Class<? extends Serializable> contentIdClass, Map<Method,ContentRepositoryExtension> extensions, ApplicationEventPublisher publisher) {
		if (extensions == null) {
			extensions = Collections.<Method, ContentRepositoryExtension>emptyMap();
		}
        this.domainClass = domainClass;
        this.contentIdClass = contentIdClass;
		this.extensions = extensions;
		this.publisher = publisher;
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		ContentRepositoryExtension extension = extensions.get(invocation.getMethod());
		if (extension != null) {
			return extension.invoke(invocation, new ContentRepositoryInvokerImpl(domainClass, contentIdClass, invocation));
		}
		
		ContentRepositoryEvent before = null;
		ContentRepositoryEvent after = null;
		
		if (getContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				before = new BeforeGetContentEvent(invocation.getArguments()[0]);
				after = new AfterGetContentEvent(invocation.getArguments()[0]);
			}
		} else if (setContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				before = new BeforeSetContentEvent(invocation.getArguments()[0]);
				after = new AfterSetContentEvent(invocation.getArguments()[0]);
			}
		} else if (unsetContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0 && invocation.getArguments()[0] != null) {
				before = new BeforeUnsetContentEvent(invocation.getArguments()[0]);
				after = new AfterUnsetContentEvent(invocation.getArguments()[0]);
			}
		}

		if (before != null) {
			publisher.publishEvent(before);
		}
		Object result;
		try {
			result = invocation.proceed();
		} catch (Exception e) {
			if (e.getMessage().contains("org.springframework.content.commons.search")) {
				throw new SearchException();
			}
			throw e;
		}

		if (after != null) {
			publisher.publishEvent(after);
		}
		return result;
	}
}

