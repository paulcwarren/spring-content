package org.springframework.content.commons.repository.factory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.AstractResourceContentRepositoryImpl;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.util.Assert;

import internal.org.springframework.content.commons.repository.factory.ContentRepositoryMethodInteceptor;

public abstract class AbstractContentStoreFactoryBean<T extends ContentStore<S, ID>, S, ID extends Serializable>
	implements InitializingBean, FactoryBean<T>, BeanClassLoaderAware, ContentStoreFactory {

	private static Log logger = LogFactory.getLog(AstractResourceContentRepositoryImpl.class);
	
	private Class<? extends ContentStore<Object, Serializable>> contentStoreInterface;
	private ClassLoader classLoader;
	
	private T contentStore;
	
	private RenditionService renditionService;

	@Autowired
	public void setRenditionService(RenditionService renditionService) {
		this.renditionService = renditionService;
	}
	
	protected RenditionService getRenditionService() {
		return this.renditionService;
	}
	
	@Required
	public void setContentStoreInterface(Class<? extends ContentStore<Object, Serializable>> contentStoreInterface) {
		Assert.notNull(contentStoreInterface);
		this.contentStoreInterface = contentStoreInterface;
	}
	
	public Class<? extends ContentStore<Object, Serializable>> getContentStoreInterface() {
		return this.contentStoreInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@SuppressWarnings("unchecked")
	public ContentStore<Object,Serializable> getContentStore() {
		return (ContentStore<Object, Serializable>) getObject();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public T getObject() {
		return initAndReturn();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends T> getObjectType() {
		return (Class<? extends T>) (null == contentStoreInterface ? ContentStore.class : contentStoreInterface);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		initAndReturn();
	}

	private T initAndReturn() {
		if (contentStore == null) {
			contentStore = createContentStore();
		}
		return contentStore;
	}

	@SuppressWarnings("unchecked")
	private T createContentStore() {
		Object target = getContentStoreImpl();
		final RenditionService renditions = this.getRenditionService();

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(new Class[] { contentStoreInterface, ContentStore.class });
		
		Map<Method, ContentRepositoryExtension> extensions = new HashMap<>();
		try {
			extensions.put(Renderable.class.getMethod("getRendition", Object.class, String.class), (ContentRepositoryExtension)renditionService);
		} catch (Exception e) {
			logger.error("Failed to setup rendition service", e);
		}
		result.addAdvice(new ContentRepositoryMethodInteceptor(extensions));

		return (T)result.getProxy(classLoader);
	}
		
	protected abstract Object getContentStoreImpl();
}
