package org.springframework.content.commons.repository.factory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import internal.org.springframework.content.commons.repository.factory.StoreMethodInterceptor;

public abstract class AbstractStoreFactoryBean
	implements InitializingBean, FactoryBean<Store<? extends Serializable>>, BeanClassLoaderAware, ApplicationEventPublisherAware, StoreFactory {

	private static Log logger = LogFactory.getLog(AbstractStoreFactoryBean.class);
	
	private Class<? extends Store<Serializable>> storeInterface;
	private ClassLoader classLoader;
	private ApplicationEventPublisher publisher;
	
	private Store<? extends Serializable> store;
	
    @Autowired(required=false)
    private Set<StoreExtension> extensions;

	@Autowired
	public void setStoreInterface(Class<? extends Store<Serializable>> storeInterface) {
		Assert.notNull(storeInterface);
		this.storeInterface = storeInterface;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.factory.ContentStoreFactory#getContentStoreInterface()
	 */
	public Class<?> getStoreInterface() {
		return this.storeInterface;
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.factory.ContentStoreFactory#getContentStore()
	 */
	@SuppressWarnings("unchecked")
	public Store<Serializable> getStore() {
		return (Store<Serializable>) getObject();
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public Store<? extends Serializable> getObject() {
		return initAndReturn();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Store<? extends Serializable>> getObjectType() {
		return (Class<? extends Store<? extends Serializable>>) (null == storeInterface ? Store.class : storeInterface);
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

	private Store<? extends Serializable> initAndReturn() {
		if (store == null) {
			store = createContentStore();
		}
		return store;
	}

	@SuppressWarnings("unchecked")
	protected Store<? extends Serializable> createContentStore() {
		Object target = getContentStoreImpl();

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(new Class[] { storeInterface, Store.class, ContentStore.class });
		
		Map<Method, StoreExtension> extensionsMap = new HashMap<>();
		try {
            for (StoreExtension extension : extensions) {
                for (Method method : extension.getMethods()) {
                    extensionsMap.put(method, extension);
                }
            }
		} catch (Exception e) {
			logger.error("Failed to setup extensions", e);
		}
		StoreMethodInterceptor intercepter = new StoreMethodInterceptor((ContentStore<Object,Serializable>)target, 
																								getDomainClass(storeInterface), 
																								getContentIdClass(storeInterface), 
																								extensionsMap, 
																								publisher);
		result.addAdvice(intercepter);

		return (Store<? extends Serializable>)result.getProxy(classLoader);
	}

    /* package */ Class<?> getDomainClass(Class<?> repositoryClass) {
        return getStoreParameter(repositoryClass, 0);
    }

    /* package */ Class<? extends Serializable> getContentIdClass(Class<?> repositoryClass) {
        return (Class<? extends Serializable>) getStoreParameter(repositoryClass, 1);
    }

    private Class<?> getStoreParameter(Class<?> repositoryClass, int index) {
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
