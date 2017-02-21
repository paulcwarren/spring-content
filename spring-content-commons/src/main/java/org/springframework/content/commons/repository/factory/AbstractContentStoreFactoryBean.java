package org.springframework.content.commons.repository.factory;

import internal.org.springframework.content.commons.repository.factory.ContentRepositoryMethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractContentStoreFactoryBean<T extends ContentStore<S, ID>, S, ID extends Serializable>
	implements InitializingBean, FactoryBean<T>, BeanClassLoaderAware, ApplicationEventPublisherAware, ContentStoreFactory {

	private static Log logger = LogFactory.getLog(AbstractContentStoreFactoryBean.class);
	
	private Class<? extends ContentStore<Object, Serializable>> contentStoreInterface;
	private ClassLoader classLoader;
	private ApplicationEventPublisher publisher;
	
	private T contentStore;
	
    @Autowired(required=false)
    private Set<ContentRepositoryExtension> extensions;

	@Autowired
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

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
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
	protected T createContentStore() {
		Object target = getContentStoreImpl();

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(new Class[] { contentStoreInterface, ContentStore.class });
		
		Map<Method, ContentRepositoryExtension> extensionsMap = new HashMap<>();
		try {
            for (ContentRepositoryExtension extension : extensions) {
                for (Method method : extension.getMethods()) {
                    extensionsMap.put(method, extension);
                }
            }
		} catch (Exception e) {
			logger.error("Failed to setup extensions", e);
		}
		result.addAdvice(new ContentRepositoryMethodInterceptor(getDomainClass(contentStoreInterface), getContentIdClass(contentStoreInterface), extensionsMap, publisher));

		return (T)result.getProxy(classLoader);
	}

    /* package */ Class<?> getDomainClass(Class<?> repositoryClass) {
        return getContentRepositoryType(repositoryClass, 0);
    }

    /* package */ Class<? extends Serializable> getContentIdClass(Class<?> repositoryClass) {
        return (Class<? extends Serializable>) getContentRepositoryType(repositoryClass, 1);
    }

    private Class<?> getContentRepositoryType(Class<?> repositoryClass, int index) {
        Class<?> clazz = null;
        Type[] types = repositoryClass.getGenericInterfaces();

        for ( Type t : types ) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (pt.getRawType().getTypeName().equals(ContentStore.class.getCanonicalName())) {
                    types = pt.getActualTypeArguments();
                    if (types.length != 2) {
                        throw new IllegalStateException(String.format("ContentRepository class %s must have domain and contentId types", repositoryClass.getCanonicalName()));
                    }
                    if (types[index] instanceof Class) {
                        clazz = (Class<?>) types[index];
                    }
                }
            }
        }
        return clazz;
    }

	protected abstract Object getContentStoreImpl();
}
