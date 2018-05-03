package internal.org.springframework.content.commons.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentName;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.io.MedializedResource;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class StoreInvokerImpl implements StoreInvoker {

	private static final Log LOGGER = LogFactory.getLog(StoreInvokerImpl.class);

	private Class<?> domainClass = null;

	private Class<? extends Serializable> contentIdClass = null;

	private Method getContentMethod = null;

	private Method getResourceMethod = null;

	private MethodInvocation invocation = null;

	public StoreInvokerImpl(Class<?> domainClass, Class<? extends Serializable> contentIdClass,
			MethodInvocation invocation) {
		Assert.notNull(domainClass, "domainClass must not be null");
		this.domainClass = domainClass;

		Assert.notNull(contentIdClass, "contentIdClass must not be null");
		this.contentIdClass = contentIdClass;

		Assert.notNull(invocation, "invocation must not be null");
		this.invocation = invocation;

		try {
			getContentMethod = ContentStore.class.getMethod("getContent", Object.class);
		} catch (Exception e) {
			LOGGER.error("Failed to get ContentStore.getContent method", e);
		}

		try {
			getResourceMethod = Store.class.getMethod("getResource", Serializable.class);
		} catch (Exception e) {
			LOGGER.error("Failed to get ContentStore.getResource method", e);
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

	@Override
	public Resource invokeGetResource() {
		try {
			Object property = invocation.getArguments()[0];
			if (property == null) {
				return null;
			}
			Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
			if (contentId == null) {
				return null;
			}
			// we may need in renderer original mime type and name so, wrap input into
			// medialized resource
			String mime = (String) BeanUtils.getFieldWithAnnotation(property, MimeType.class);
			if (mime == null) {
				mime = "application/octet-stream";
			}

			String name = (String) BeanUtils.getFieldWithAnnotation(property, ContentName.class);
			if (name == null) {
				name = "";
			}

			return new MedializedResource((Resource) this.getResourceMethod.invoke(invocation.getThis(), contentId),
					mime, name);
		} catch (IllegalAccessException e) {
			LOGGER.error(String.format("Unable to get content for inovcation", invocation.getMethod().getName()), e);
		} catch (InvocationTargetException e) {
			LOGGER.error(String.format("Unable to get content for inovcation", invocation.getMethod().getName()), e);
		}
		return null;
	}
}
