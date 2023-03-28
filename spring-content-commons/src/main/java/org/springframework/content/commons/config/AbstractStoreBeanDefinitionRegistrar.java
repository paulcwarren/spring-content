package org.springframework.content.commons.config;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragmentDefinition;
import internal.org.springframework.content.commons.config.StoreFragmentDetector;
import internal.org.springframework.content.commons.config.StoreFragmentsFactoryBean;
import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;
import internal.org.springframework.content.commons.utils.StoreCandidateComponentProvider;
import internal.org.springframework.content.commons.utils.StoreUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.StoreInterfaceUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Pair;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStoreBeanDefinitionRegistrar
		implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware {

	private static final Log LOGGER = LogFactory.getLog(AbstractStoreBeanDefinitionRegistrar.class);

	public static final String STORE_INTERFACE_PROPERTY = "storeInterface";
	public static final String DOMAIN_CLASS_PROPERTY = "domainClass";
	public static final String ID_CLASS_PROPERTY = "idClass";
	public static final String STORE_INTERFACE_CLASS_PROPERTY = "storeInterfaceClass";

	private static String REPOSITORY_INTERFACE_POST_PROCESSOR = "internal.org.springframework.content.commons.utils.StoreInterfaceAwareBeanPostProcessor";

	private Environment environment;
	private ResourceLoader resourceLoader;
	private BeanFactory beanFactory;
	private boolean multiStoreMode;

	@Override
    public void setEnvironment(Environment env) {
		this.environment = env;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	@Override
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
	@Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Assert.notNull(importingClassMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.isTrue(registry instanceof ConfigurableListableBeanFactory, "BeanDefinitionRegistry must be instance of ConfigurableListableBeanFactory");

		// Guard against calls for sub-classes
		// if (importingClassMetadata.getAnnotationAttributes(getAnnotation().getName())
		// == null) {
		// return;
		// }

		BeanDefinition annotatedStoreEventHandlerDef = createBeanDefinition(AnnotatedStoreEventInvoker.class);
		if (registry.containsBeanDefinition("annotatedStoreEventHandler") == false) {
			registry.registerBeanDefinition("annotatedStoreEventHandler", annotatedStoreEventHandlerDef);
		}

		createOperationsBean(registry);

		registerContentStoreBeanDefinitions(importingClassMetadata, registry);
	}

	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationAttributes attributes = new AnnotationAttributes(importingClassMetadata.getAnnotationAttributes(getAnnotation().getName()));
		String[] basePackages = this.getBasePackages(attributes, importingClassMetadata);

        StoreCandidateComponentProvider scanner = new StoreCandidateComponentProvider(false, environment);
        scanner.setResourceLoader(resourceLoader);

        Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(
                scanner,
                environment,
                resourceLoader,
                basePackages,
                multipleStoreImplementationsDetected(),
                this.getSignatureTypes(),
                this.getOverridePropertyValue());

		buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected void buildAndRegisterDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, AnnotationAttributes attributes, String[] basePackages, Set<GenericBeanDefinition> definitions) {
		for (BeanDefinition definition : definitions) {

			String factoryBeanName = StoreUtils.getStoreFactoryBeanName(attributes);
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

			builder.getRawBeanDefinition().setSource(importingClassMetadata);

			Class<? extends Store> storeClass = null;
			try {
				storeClass = loadStoreClass((ConfigurableListableBeanFactory)registry, definition);
			}
			catch (ClassNotFoundException e) {
				LOGGER.error(format("Instantiating store class %s", storeClass.getName()), e);
			}

			builder.addConstructorArgValue(storeClass);

			MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);

			String storeInterface = definition.getBeanClassName();
			String[] interfaces = new String[0];
			try {
				interfaces = metadataReaderFactory.getMetadataReader(storeInterface).getClassMetadata().getInterfaceNames();
			}
			catch (IOException e) {
				LOGGER.error(format("Reading store interface %s", storeInterface), e);
			}

			StoreFragmentDetector detector = new StoreFragmentDetector(environment, resourceLoader,"Impl", basePackages, metadataReaderFactory);

			Pair<Optional<Class<?>>, Class<? extends Serializable>> types = StoreInterfaceUtils.getStoreTypes(storeClass);

			final Class<?> domainClass = types.getFirst().isPresent() ? types.getFirst().get() : null;
			final Class<?> idClass = types.getSecond();

			Predicate isCandidate = new IsCandidatePredicate(this.getSignatureTypes());

			final Class<? extends Store> storeClassFinal = storeClass;
			List<String> fragmentBeanNames = Arrays.stream(interfaces)
					.filter(isCandidate::test)
					.map(iface -> detector.detectCustomImplementation(iface, storeInterface))
					.peek(it -> registerStoreFragmentImplementation(registry, importingClassMetadata, it, storeClassFinal, domainClass, idClass))
					.peek(it -> registerStoreFragment(registry, importingClassMetadata, it))
					.map(it -> it.getFragmentBeanName())
					.collect(Collectors.toList());

			BeanDefinitionBuilder fragmentsBuilder = BeanDefinitionBuilder.rootBeanDefinition(StoreFragmentsFactoryBean.class);
			fragmentsBuilder.addConstructorArgValue(fragmentBeanNames);
			registry.registerBeanDefinition(StoreUtils.getStoreBeanName(definition) + "#StoreFragments", fragmentsBuilder.getBeanDefinition());

			builder.addPropertyValue("storeFragments", ParsingUtils.getSourceBeanDefinition(fragmentsBuilder, importingClassMetadata));

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

	protected Class<? extends Store> loadStoreClass(ConfigurableListableBeanFactory registry, BeanDefinition definition) throws ClassNotFoundException {
		Class<?> candidateStoreClass = ClassUtils.forName(definition.getBeanClassName(), registry.getBeanClassLoader());
		if (!Store.class.isAssignableFrom(candidateStoreClass) && !ReactiveContentStore.class.isAssignableFrom(candidateStoreClass) &&
			!org.springframework.content.commons.repository.Store.class.isAssignableFrom(candidateStoreClass) && !org.springframework.content.commons.repository.ReactiveContentStore.class.isAssignableFrom(candidateStoreClass)) {
			throw new IllegalStateException(String.format("Store class %s is not assignable from Store or ReactiveContentStore", definition.getBeanClassName()));
		}
		return (Class<? extends Store>) candidateStoreClass;
	}

	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		return;
	}

	private void registerStoreFragmentImplementation(BeanDefinitionRegistry registry, AnnotationMetadata source, StoreFragmentDefinition fragmentDefinition, Class<?> storeInterfaceClass, Class<?> domainClass, Class<?> idClass) {

		String beanName = fragmentDefinition.getImplementationBeanName();

		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		((AbstractBeanDefinition)fragmentDefinition.getBeanDefinition()).setSource(source);

		try {
            Class<?> ifaceClass = ClassUtils.forName(fragmentDefinition.getInterfaceName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
            Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
            Class<?> storeClass = ClassUtils.forName(fragmentDefinition.getStoreInterfaceName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());

            Method method = ReflectionUtils.findMethod(implClass, "setGenericArguments", Class[].class);
            if (method != null) {
                List<TypeInformation<?>> types = ClassTypeInformation.from(storeClass).getSuperTypeInformation(ifaceClass).getTypeArguments();
                List<Class<?>> genericArguments = types.stream().map(TypeInformation::getType).collect(toList());
                fragmentDefinition.getBeanDefinition().getPropertyValues().add("genericArguments", genericArguments.toArray(new Class[] {}));
            }
		}
        catch (ClassNotFoundException e) {

            LOGGER.error("Failed setting fragment generic arguments", e);
        }

		try {
			Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
			Method method = ReflectionUtils.findMethod(implClass, "setDomainClass", Class.class);
			if (method != null) {
				fragmentDefinition.getBeanDefinition().getPropertyValues().add(DOMAIN_CLASS_PROPERTY, domainClass);
			}
		}
		catch (ClassNotFoundException e) {

            LOGGER.error("Failed setting fragment domain class", e);
		}

		try {
			Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
			Method method = ReflectionUtils.findMethod(implClass, "setIdClass", Class.class);
			if (method != null) {
				fragmentDefinition.getBeanDefinition().getPropertyValues().add(ID_CLASS_PROPERTY, idClass);
			}
		}
		catch (ClassNotFoundException e) {

            LOGGER.error("Failed setting fragment ID class", e);
		}

		try {
			Class<?> implClass = ClassUtils.forName(fragmentDefinition.getBeanDefinition().getBeanClassName(), ((ConfigurableListableBeanFactory)registry).getBeanClassLoader());
			Method method = ReflectionUtils.findMethod(implClass, "setStoreInterfaceClass", Class.class);
			if (method != null) {
				fragmentDefinition.getBeanDefinition().getPropertyValues().add(STORE_INTERFACE_CLASS_PROPERTY, storeInterfaceClass);
			}
		}
		catch (ClassNotFoundException e) {

			LOGGER.error("Failed setting fragment ID class", e);
		}

		registry.registerBeanDefinition(beanName, fragmentDefinition.getBeanDefinition());
	}

	private void registerStoreFragment(BeanDefinitionRegistry registry, AnnotationMetadata source, StoreFragmentDefinition fragmentDefinition) {

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

	protected boolean multipleStoreImplementationsDetected() {

		boolean multipleOldModulesFound = SpringFactoriesLoader.loadFactoryNames(org.springframework.content.commons.repository.factory.AbstractStoreFactoryBean.class, resourceLoader.getClassLoader()).size() > 1;
		boolean multipleNewModulesFound = SpringFactoriesLoader.loadFactoryNames(AbstractStoreFactoryBean.class, resourceLoader.getClassLoader()).size() > 1;

		if (multipleOldModulesFound == true || multipleNewModulesFound == true) {
			LOGGER.info("Multiple store modules detected.  Entering strict resolution mode");
		}

		return multipleOldModulesFound == true || multipleNewModulesFound == true;
	}

	private class IsCandidatePredicate implements Predicate<String> {

		private Class<?>[] additionalTypes;

		public IsCandidatePredicate(Class<?>[] additionalTypes) {
			this.additionalTypes = additionalTypes;
		}

		@Override
		public boolean test(String s) {

			if (org.springframework.content.commons.repository.Store.class.getName().equals(s) ||
				org.springframework.content.commons.repository.AssociativeStore.class.getName().equals(s) ||
				org.springframework.content.commons.repository.ContentStore.class.getName().equals(s) ||
				org.springframework.content.commons.repository.ReactiveContentStore.class.getName().equals(s) ||
				Store.class.getName().equals(s) ||
				AssociativeStore.class.getName().equals(s) ||
				ContentStore.class.getName().equals(s) ||
				ReactiveContentStore.class.getName().equals(s) ||
				s.startsWith("java.")) {

				return false;
			}

			for (Class<?> additionalType : additionalTypes) {
				if (additionalType.getName().equals(s)) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Return the annotation to obtain configuration information from
	 * @return configuration annotation
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * Return the storage module's signature types
	 * @return array of classes that represent signature types
	 */
	protected abstract Class<?>[] getSignatureTypes();

    /**
     * Return the storage module's override property value
     * @return the storage modules' override property value
     */
    protected String getOverridePropertyValue() {
        return "";
    }
}
