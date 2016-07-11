package org.springframework.content.common.repository.factory;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.content.annotations.MimeType;
import org.springframework.content.common.renditions.Renderable;
import org.springframework.content.common.renditions.RenditionService;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.utils.BeanUtils;
import org.springframework.util.Assert;

public abstract class AbstractContentStoreFactoryBean<T extends ContentStore<S, ID>, S, ID extends Serializable>
	implements InitializingBean, FactoryBean<T>, BeanClassLoaderAware, ContentStoreFactory {

	//private AbstractContentStoreFactory factory;

	//private ContentStoreService contentStoreService;
	private Class<? extends ContentStore<Object, Serializable>> contentStoreInterface;
	private ClassLoader classLoader;
	
	private T contentStore;
	
	private RenditionService renditionService;

	/*public ContentStoreService getContentStoreService() {
		return contentStoreService;
	}

	@Autowired
	public void setContentStoreService(ContentStoreService contentStoreService) {
		this.contentStoreService = contentStoreService;
	}*/

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
		result.addAdvice(new MethodInterceptor() {

			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				Class<?> clazz  = Renderable.class;
				final Method getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
				Class<?> storeClazz  = ContentStore.class;
				final Method getContentMethod = storeClazz.getMethod("getContent", Object.class);
				final Method convertMethod = renditions.getClass().getMethod("convert", String.class, InputStream.class, String.class);

				if (invocation.getMethod().equals(getRenditionMethod)) {
					try {
						String fromMimeType = null;
						if (BeanUtils.hasFieldWithAnnotation(invocation.getArguments()[0], MimeType.class)) {
							fromMimeType = (String)BeanUtils.getFieldWithAnnotation(invocation.getArguments()[0], MimeType.class);
						}
						String toMimeType = (String) invocation.getArguments()[1];
						
						if (renditions.canConvert(fromMimeType, toMimeType)) {
							InputStream content = (InputStream) getContentMethod.invoke(invocation.getThis(), invocation.getArguments()[0]);
							InputStream rendition = (InputStream) convertMethod.invoke(renditions, fromMimeType, content, toMimeType);
							return rendition;
						} else {
							return null;
						}
					} catch (Exception e) {
						e.printStackTrace(System.out);
					}
				}
				return invocation.proceed();
			}});

		return (T)result.getProxy(classLoader);
	}
		
	protected abstract Object getContentStoreImpl();
}
