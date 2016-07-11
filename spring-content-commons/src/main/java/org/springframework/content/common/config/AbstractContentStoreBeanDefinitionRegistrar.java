package org.springframework.content.common.config;

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

import internal.org.springframework.content.common.renditions.RenditionServiceImpl;
import internal.org.springframework.content.common.storeservice.ContentStoreServiceImpl;
import internal.org.springframework.content.common.utils.ContentRepositoryUtils;

public abstract class AbstractContentStoreBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanFactoryAware {

	private static String REPOSITORY_INTERFACE_POST_PROCESSOR = "internal.org.springframework.content.common.utils.ContentRepositoryInterfaceAwareBeanPostProcessor";
	
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
		
		BeanDefinition storeServiceBeanDef = createContentStoreServiceBeanDefinition();
		registry.registerBeanDefinition("contentStoreService", storeServiceBeanDef);

		BeanDefinition renditionServiceBeanDef = createRenditionServiceBeanDefinition();
		registry.registerBeanDefinition("renditionService", renditionServiceBeanDef);

//		BeanDefinition renditionProviderBeanDef = createRenditionProviderBeanDefinition(PdfRenditionProvider.class);
//		registry.registerBeanDefinition("pdfRenditionProvider", renditionProviderBeanDef);
//
//		BeanDefinition htmlRenditionProviderBeanDef = createRenditionProviderBeanDefinition(WordToHtmlRenditionProvider.class);
//		registry.registerBeanDefinition("htmlRenditionProvider", htmlRenditionProviderBeanDef);

		registerContentStoreBeanDefinitions(importingClassMetadata, registry);
	}

	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes attributes = new AnnotationAttributes(importingClassMetadata.getAnnotationAttributes(getAnnotation().getName()));
		String[] basePackages = this.getBasePackages(attributes, importingClassMetadata);
		
		Set<GenericBeanDefinition> definitions = ContentRepositoryUtils.getContentRepositoryCandidates(resourceLoader, basePackages);

		for (BeanDefinition definition : definitions) {
		
			String factoryBeanName = ContentRepositoryUtils.getRepositoryFactoryBeanName(attributes);

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

			builder.getRawBeanDefinition().setSource(importingClassMetadata);
			builder.addPropertyValue("contentStoreInterface", definition.getBeanClassName());
			
			registry.registerBeanDefinition(ContentRepositoryUtils.getRepositoryBeanName(definition), builder.getBeanDefinition());
		}
	}
	
	// default implementation for non-autoconfigured clients
	protected String[] getBasePackages(AnnotationAttributes attributes, AnnotationMetadata importingClassMetadata) {
		return ContentRepositoryUtils.getBasePackages(attributes, /* default*/ new String[] { ClassUtils.getPackageName(importingClassMetadata.getClassName()) });
	}
	
	private BeanDefinition createContentStoreServiceBeanDefinition() {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(ContentStoreServiceImpl.class);

		MutablePropertyValues values = new MutablePropertyValues();
		beanDef.setPropertyValues(values);
		
		return beanDef;
	}

	private BeanDefinition createRenditionServiceBeanDefinition() {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(RenditionServiceImpl.class);

		MutablePropertyValues values = new MutablePropertyValues();
		beanDef.setPropertyValues(values);
		
		return beanDef;
	}

//	private BeanDefinition createRenditionProviderBeanDefinition(Class<? extends RenditionProvider> providerClass) {
//		GenericBeanDefinition beanDef = new GenericBeanDefinition();
//		beanDef.setBeanClass(providerClass);
//
//		MutablePropertyValues values = new MutablePropertyValues();
//		beanDef.setPropertyValues(values);
//		
//		return beanDef;
//	}

	/**
	 * Return the annotation to obtain configuration information from. Will be wrappen into an
	 * {@link AnnotationRepositoryConfigurationSource} so have a look at the constants in there for what annotation
	 * attributes it expects.
	 * 
	 * @return
	 */
	protected abstract Class<? extends Annotation> getAnnotation();
}
