package internal.org.springframework.content.commons.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.repository.ContentRepositoryInvoker;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.util.Assert;

public class ContentRepositoryInvokerImpl implements ContentRepositoryInvoker {

	private static final Log LOGGER = LogFactory.getLog(ContentRepositoryInvokerImpl.class);

	private Class<?> domainClass = null;

	private Class<? extends Serializable> contentIdClass = null;

	private Method getContentMethod = null;

	private MethodInvocation invocation = null;


	public ContentRepositoryInvokerImpl(Class<?> domainClass, Class<? extends Serializable> contentIdClass, MethodInvocation invocation) {
		Assert.notNull(domainClass, "domainClass must not be null");
		this.domainClass = domainClass;

		Assert.notNull(contentIdClass, "contentIdClass must not be null");
		this.contentIdClass = contentIdClass;

		Assert.notNull(invocation, "invocation must not be null");
		this.invocation = invocation;

		try {
			Class<?> storeClazz  = ContentStore.class;
			getContentMethod = storeClazz.getMethod("getContent", Object.class);
		} catch (Exception e) {
			LOGGER.error("Failed to get ContentStore.getContentmethod", e);
		}
	}

	@Override
	public Class<?> getDomainClass() {
		return domainClass;
	}

	@Override
	public Class<? extends Serializable> getContentIdClass() {
		return contentIdClass;
	}

	@Override
	public InputStream invokeGetContent() {
		try {
			return (InputStream) this.getContentMethod.invoke(invocation.getThis(), invocation.getArguments()[0]);
		} catch (IllegalAccessException e) {
			LOGGER.error(String.format("Unable to get content for inovcation", invocation.getMethod().getName()), e);
		} catch (InvocationTargetException e) {
			LOGGER.error(String.format("Unable to get content for inovcation", invocation.getMethod().getName()), e);
		}
		return null;
	}
}
