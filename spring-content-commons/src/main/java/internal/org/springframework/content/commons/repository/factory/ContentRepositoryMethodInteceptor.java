package internal.org.springframework.content.commons.repository.factory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.context.ApplicationEventPublisher;

import internal.org.springframework.content.commons.repository.ContentRepositoryInvokerImpl;

public class ContentRepositoryMethodInteceptor implements MethodInterceptor {

	private Map<Method,ContentRepositoryExtension> extensions;
	private ApplicationEventPublisher publisher;
	
	public ContentRepositoryMethodInteceptor(Map<Method,ContentRepositoryExtension> extensions, ApplicationEventPublisher publisher) {
		if (extensions == null) {
			extensions = Collections.<Method, ContentRepositoryExtension>emptyMap();
		}
		this.extensions = extensions;
		this.publisher = publisher;
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		ContentRepositoryExtension extension = extensions.get(invocation.getMethod());
		if (extension != null) {
			return extension.invoke(invocation, new ContentRepositoryInvokerImpl(invocation));
		}
		return invocation.proceed();
	}
}
