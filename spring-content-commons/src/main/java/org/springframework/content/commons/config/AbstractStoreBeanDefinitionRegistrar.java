package org.springframework.content.commons.config;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;
import internal.org.springframework.content.commons.storeservice.ContentStoreServiceImpl;
import internal.org.springframework.content.commons.utils.StoreUtils;

public abstract class AbstractStoreBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanFactoryAware {

	public static final String STORE_INTERFACE_PROPERTY = "storeInterface";

	private static String REPOSITORY_INTERFACE_POST_PROCESSOR = "internal.org.springframework.content.commons.utils.StoreInterfaceAwareBeanPostProcessor";
	
	private ResourceLoader resourceLoader;
	private BeanFactory beanFactory;
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}
	
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Assert.notNull(importingClassMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		// Guard against calls for sub-classes
		//if (importingClassMetadata.getAnnotationAttributes(getAnnotation().getName()) == null) {
		//	return;
		//}

		RootBeanDefinition repositoryInterfacePostProcessor = new RootBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR);
		repositoryInterfacePostProcessor.setSource(importingClassMetadata);
		registry.registerBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR, repositoryInterfacePostProcessor);
		
		BeanDefinition storeServiceBeanDef = createBeanDefinition(ContentStoreServiceImpl.class);
		registry.registerBeanDefinition("contentStoreService", storeServiceBeanDef);

		BeanDefinition annotatedStoreEventHandlerDef = createBeanDefinition(AnnotatedStoreEventInvoker.class);
		registry.registerBeanDefinition("annotatedStoreEventHandler", annotatedStoreEventHandlerDef);

		BeanDefinition renditionServiceBeanDef = createBeanDefinition(RenditionServiceImpl.class);
		registry.registerBeanDefinition("renditionService", renditionServiceBeanDef);

		createOperationsBean(registry);
		
		registerContentStoreBeanDefinitions(importingClassMetadata, registry);
	}

	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes attributes = new AnnotationAttributes(importingClassMetadata.getAnnotationAttributes(getAnnotation().getName()));
		String[] basePackages = this.getBasePackages(attributes, importingClassMetadata);
		
		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(resourceLoader, basePackages);

		for (BeanDefinition definition : definitions) {
		
			String factoryBeanName = StoreUtils.getStoreFactoryBeanName(attributes);

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

			builder.getRawBeanDefinition().setSource(importingClassMetadata);
			builder.addPropertyValue(STORE_INTERFACE_PROPERTY, definition.getBeanClassName());
			
			registry.registerBeanDefinition(StoreUtils.getStoreBeanName(definition), builder.getBeanDefinition());
		}
	}
	
	// default implementation for non-autoconfigured clients
	protected String[] getBasePackages(AnnotationAttributes attributes, AnnotationMetadata importingClassMetadata) {
		return StoreUtils.getBasePackages(attributes, /* default*/ new String[] { ClassUtils.getPackageName(importingClassMetadata.getClassName()) });
	}
	
	protected BeanDefinition createBeanDefinition(Class<?> beanType) {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(beanType);

		MutablePropertyValues values = new MutablePropertyValues();
		beanDef.setPropertyValues(values);
		
		return beanDef;
	}

	protected void createOperationsBean(BeanDefinitionRegistry registry) { return; }
	
	/**
	 * Return the annotation to obtain configuration information from
	 * @return 
	 * 		configuration annotation
	 */
	protected abstract Class<? extends Annotation> getAnnotation();
}
