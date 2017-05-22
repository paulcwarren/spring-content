package internal.org.springframework.content.commons.repository.factory;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import internal.org.springframework.content.commons.repository.StoreInvokerImpl;

public class StoreMethodInterceptor implements MethodInterceptor {

	private Map<Method,StoreExtension> extensions;
	private ContentStore<Object, Serializable> store = null;
	private ApplicationEventPublisher publisher;
	
	private static Method getContentMethod; 
	private static Method setContentMethod; 
	private static Method unsetContentMethod;
	private static Method getResourceMethod;
	private static Method associateResourceMethod;
	private static Method unassociateResourceMethod;
    private Class<?> domainClass = null;
    private Class<? extends Serializable> contentIdClass = null;
	
	static {
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
		Assert.notNull(getContentMethod);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
		Assert.notNull(setContentMethod);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
		Assert.notNull(unsetContentMethod);
		getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource", Serializable.class);
		Assert.notNull(getResourceMethod);
		associateResourceMethod = ReflectionUtils.findMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
		Assert.notNull(getResourceMethod);
		unassociateResourceMethod = ReflectionUtils.findMethod(AssociativeStore.class, "unassociate", Object.class);
		Assert.notNull(getResourceMethod);
	}
	
	public StoreMethodInterceptor(ContentStore<Object, Serializable> store, Class<?> domainClass, Class<? extends Serializable> contentIdClass, Map<Method,StoreExtension> extensions, ApplicationEventPublisher publisher) {
		if (extensions == null) {
			extensions = Collections.<Method, StoreExtension>emptyMap();
		}
		this.store = store;
        this.domainClass = domainClass;
        this.contentIdClass = contentIdClass;
		this.extensions = extensions;
		this.publisher = publisher;
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		StoreExtension extension = extensions.get(method);
		if (extension != null) {
			return extension.invoke(invocation, new StoreInvokerImpl(domainClass, contentIdClass, invocation));
		} else {
			if (!isStoreMethod(invocation)) {
				throw new StoreAccessException(String.format("No implementation found for %s", method.getName()));
			}
		}
		
		StoreEvent before = null;
		StoreEvent after = null;
		
		if (getContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				before = new BeforeGetContentEvent(invocation.getArguments()[0], store);
				after = new AfterGetContentEvent(invocation.getArguments()[0], store);
			}
		} else if (setContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0) {
				before = new BeforeSetContentEvent(invocation.getArguments()[0], store);
				after = new AfterSetContentEvent(invocation.getArguments()[0], store);
			}
		} else if (unsetContentMethod.equals(invocation.getMethod())) {
			if (invocation.getArguments().length > 0 && invocation.getArguments()[0] != null) {
				before = new BeforeUnsetContentEvent(invocation.getArguments()[0], store);
				after = new AfterUnsetContentEvent(invocation.getArguments()[0], store);
			}
		}

		if (before != null) {
			publisher.publishEvent(before);
		}
		Object result;
		try {
			result = invocation.proceed();
		} catch (Exception e) {
			throw e;
		}

		if (after != null) {
			publisher.publishEvent(after);
		}
		return result;
	}

	private boolean isStoreMethod(MethodInvocation invocation) {
		if (getContentMethod.equals(invocation.getMethod()) || 
			setContentMethod.equals(invocation.getMethod()) || 
			unsetContentMethod.equals(invocation.getMethod()) || 
			getResourceMethod.equals(invocation.getMethod()) ||
			associateResourceMethod.equals(invocation.getMethod()) || 
			unassociateResourceMethod.equals(invocation.getMethod())) {
			return true;
		}
		return false;
	}
}

