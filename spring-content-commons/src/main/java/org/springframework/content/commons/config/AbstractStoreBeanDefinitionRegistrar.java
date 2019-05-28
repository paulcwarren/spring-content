package org.springframework.content.commons.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragmentDetector;
import internal.org.springframework.content.commons.config.StoreFragmentsFactoryBean;
import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;
import internal.org.springframework.content.commons.storeservice.ContentStoreServiceImpl;
import internal.org.springframework.content.commons.utils.StoreUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.config.ParsingUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractStoreBeanDefinitionRegistrar
		implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware {

	public static final String STORE_INTERFACE_PROPERTY = "storeInterface";
	public static final String DOMAIN_CLASS_PROPERTY = "domainClass";
	public static final String ID_CLASS_PROPERTY = "idClass";

	private static String REPOSITORY_INTERFACE_POST_PROCESSOR = "internal.org.springframework.content.commons.utils.StoreInterfaceAwareBeanPostProcessor";

	private Environment environment;
	private ResourceLoader resourceLoader;
	private BeanFactory beanFactory;

	public void setEnvironment(Environment env) {
		this.environment = env;
	}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#
	 * registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata,
	 * org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Assert.notNull(importingClassMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.isTrue(registry instanceof ConfigurableListableBeanFactory, "BeanDefinitionRegistry must be instance of ConfigurableListableBeanFactory");

		// Guard against calls for sub-classes
		// if (importingClassMetadata.getAnnotationAttributes(getAnnotation().getName())
		// == null) {
		// return;
		// }

		RootBeanDefinition repositoryInterfacePostProcessor = new RootBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR);
		repositoryInterfacePostProcessor.setSource(importingClassMetadata);
		if (registry.containsBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR) == false) {
			registry.registerBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR,repositoryInterfacePostProcessor);
		}

		BeanDefinition storeServiceBeanDef = createBeanDefinition(ContentStoreServiceImpl.class);
		if (registry.containsBeanDefinition("contentStoreService") == false) {
			registry.registerBeanDefinition("contentStoreService", storeServiceBeanDef);
		}

		BeanDefinition annotatedStoreEventHandlerDef = createBeanDefinition(AnnotatedStoreEventInvoker.class);
		if (registry.containsBeanDefinition("annotatedStoreEventHandler") == false) {
			registry.registerBeanDefinition("annotatedStoreEventHandler", annotatedStoreEventHandlerDef);
		}

//		BeanDefinition renditionServiceBeanDef = createBeanDefinition(RenditionServiceImpl.class);
//		if (registry.containsBeanDefinition("renditionService") == false) {
//			registry.registerBeanDefinition("renditionService", renditionServiceBeanDef);
//		}

		createOperationsBean(registry);

		registerContentStoreBeanDefinitions(importingClassMetadata, registry);
	}

	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationAttributes attributes = new AnnotationAttributes(importingClassMetadata.getAnnotationAttributes(getAnnotation().getName()));
		String[] basePackages = this.getBasePackages(attributes, importingClassMetadata);

		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(resourceLoader, basePackages);

		buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected void buildAndRegisterDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, AnnotationAttributes attributes, String[] basePackages, Set<GenericBeanDefinition> definitions) {
		for (BeanDefinition definition : definitions) {

			String factoryBeanName = StoreUtils.getStoreFactoryBeanName(attributes);
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

			builder.getRawBeanDefinition().setSource(importingClassMetadata);
			builder.addPropertyValue(STORE_INTERFACE_PROPERTY, definition.getBeanClassName());

			/*
			 * Refactored store extension implementation
			 *
			 * - Import registrar StoreBeanDefinitionBuilder finds impls and match against store each interface  	Done
			 * - For each creates a StoreFragment (bean?)															Done
			 * - StoreFragments is an additional wired prop into the StoreFactoryBean								Done
			 * - Store is instantiated with StoreFragments															Done
			 * - Store passes StoreFragments into the StoreMethodInteceptor											Done
			 * - Figure out how to get into like domainClass into the StoreFragment implementation
			 * - Wire StoreFragments in instead of StoreExtension
			 */
			MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);

			String storeInterface = definition.getBeanClassName();

			// TODO: detectCustomImplementation needs to return a fragment specification
			StoreFragmentDetector detector = new StoreFragmentDetector(environment, resourceLoader,"Impl", basePackages, metadataReaderFactory);
			try {
				final Class<?> domainClass = AbstractStoreFactoryBean.getDomainClass(ClassUtils
						.forName(definition.getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader()));
				final Class<?> idClass = AbstractStoreFactoryBean.getContentIdClass(ClassUtils.forName(definition.getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader()));

				String[] interfaces = metadataReaderFactory.getMetadataReader(storeInterface).getClassMetadata().getInterfaceNames();

				Predicate isCandidate = new IsCandidatePredicate();

				List<String> fragmentBeanNames = Arrays.stream(interfaces)
						.filter(isCandidate::test)
						.map(iface -> detector.detectCustomImplementation(iface, storeInterface))
						.peek(it -> registerStoreFragmentImplementation(registry, importingClassMetadata, it, domainClass, idClass))
						.peek(it -> registerStoreFragment(registry, importingClassMetadata, it))
						.map(it -> it.getFragmentBeanName())
						.collect(Collectors.toList());

				BeanDefinitionBuilder fragmentsBuilder = BeanDefinitionBuilder.rootBeanDefinition(StoreFragmentsFactoryBean.class);

				fragmentsBuilder.addConstructorArgValue(fragmentBeanNames);

				builder.addPropertyValue("storeFragments", ParsingUtils.getSourceBeanDefinition(fragmentsBuilder, importingClassMetadata));

				int i=0;
			}
			catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}

			/*
			 *
			 */

			registry.registerBeanDefinition(StoreUtils.getStoreBeanName(definition), builder.getBeanDefinition());
		}
	}

	// default implementation for non-autoconfigured clients
	protected String[] getBasePackages(AnnotationAttributes attributes,
			AnnotationMetadata importingClassMetadata) {
		return StoreUtils.getBasePackages(attributes, /* default */ new String[] {
				ClassUtils.getPackageName(importingClassMetadata.getClassName()) });
	}

	protected BeanDefinition createBeanDefinition(Class<?> beanType) {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(beanType);

		MutablePropertyValues values = new MutablePropertyValues();
		beanDef.setPropertyValues(values);

		return beanDef;
	}

	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		return;
	}

	private void registerStoreFragmentImplementation(BeanDefinitionRegistry registry, AnnotationMetadata source, FragmentDefinition fragmentDefinition, Class<?> domainClass, Class<?> idClass) {

		String beanName = fragmentDefinition.getImplementationBeanName();

		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		((AbstractBeanDefinition)fragmentDefinition.getBeanDefinition()).setSource(source);

		try {
			Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
			Method method = ReflectionUtils.findMethod(implClass, "setDomainClass", Class.class);
			if (method != null) {
				fragmentDefinition.getBeanDefinition().getPropertyValues().add(DOMAIN_CLASS_PROPERTY, domainClass);
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
			Method method = ReflectionUtils.findMethod(implClass, "setIdClass", Class.class);
			if (method != null) {
				fragmentDefinition.getBeanDefinition().getPropertyValues().add(ID_CLASS_PROPERTY, idClass);
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		registry.registerBeanDefinition(beanName, fragmentDefinition.getBeanDefinition());
	}

	private void registerStoreFragment(BeanDefinitionRegistry registry, AnnotationMetadata source, FragmentDefinition fragmentDefinition) {

		String implementationBeanName = fragmentDefinition.getImplementationBeanName();
		String fragmentBeanName = fragmentDefinition.getFragmentBeanName();

		if (registry.containsBeanDefinition(fragmentBeanName)) {
			return;
		}

		BeanDefinitionBuilder fragmentBuilder = BeanDefinitionBuilder.genericBeanDefinition(StoreFragment.class);

		fragmentBuilder.addConstructorArgValue(fragmentDefinition.getInterfaceName());
		fragmentBuilder.addConstructorArgReference(implementationBeanName);

		registry.registerBeanDefinition(fragmentBeanName, ParsingUtils.getSourceBeanDefinition(fragmentBuilder, source));
	}

	private String getImplementationBeanName(BeanDefinition beanDef) {
		String beanClassName = ConfigurationUtils.getRequiredBeanClassName(beanDef);
		return StringUtils.uncapitalize(ClassUtils.getShortName(beanClassName));
	}

	public static class FragmentDefinition {

		private final String interfaceName;
		private final String implementationClassName;
		private final BeanDefinition beanDefinition;

		private String storeInterfaceName;

		public FragmentDefinition(String interfaceName, BeanDefinition beanDef) {
			this.interfaceName = interfaceName;
			this.implementationClassName = ConfigurationUtils.getRequiredBeanClassName(beanDef);
			this.beanDefinition = beanDef;
		}

		public void setStoreInterfaceName(String storeInterface) {
			this.storeInterfaceName = storeInterface;
		}

		public String getStoreInterfaceName() {
			return this.storeInterfaceName;
		}

		String getInterfaceName() {
			return interfaceName;
		}

		public String getImplementationBeanName() {
			return this.storeInterfaceName + "#" + StringUtils.uncapitalize(ClassUtils.getShortName(implementationClassName));
		}

		BeanDefinition getBeanDefinition() {
			return beanDefinition;
		}

		public String getFragmentBeanName() {
			return getImplementationBeanName() + "Fragment";
		}

	}

	private class IsCandidatePredicate implements Predicate<String> {

		@Override
		public boolean test(String s) {

			if (Store.class.getName().equals(s) ||
				AssociativeStore.class.getName().equals(s) ||
				ContentStore.class.getName().equals(s) ||
				s.startsWith("java.")) {

				return false;
			}

			return true;
		}
	}

	/**
	 * Return the annotation to obtain configuration information from
	 * @return configuration annotation
	 */
	protected abstract Class<? extends Annotation> getAnnotation();
}
