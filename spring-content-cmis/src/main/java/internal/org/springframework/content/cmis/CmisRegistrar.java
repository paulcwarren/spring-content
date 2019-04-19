package internal.org.springframework.content.cmis;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.UUID;

import internal.org.springframework.content.commons.utils.StoreUtils;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.content.cmis.CmisDocument;
import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.EnableCmis;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.SpringDataAnnotationBeanNameGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static java.lang.String.format;

public class CmisRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware, BeanClassLoaderAware {

	public static final String CMIS_REPOSITORY_INFO = "cmisRepositoryInfo";
	private static final String CMIS_TYPE_DEFINITION_LIST = "cmisTypeDefinitionList";

	private @SuppressWarnings("null") /*@Nonnull*/ ResourceLoader resourceLoader;
	private @SuppressWarnings("null") /*@Nonnull*/ Environment environment;
	private ClassLoader classLoader;

	BeanDefinition cmisFolderBeanDefinition;
	BeanDefinition cmisFolderRepositoryBeanDefinition;
	BeanDefinition cmisDocumentBeanDefinition;
	BeanDefinition cmisDocumentRepositoryBeanDefinition;
	BeanDefinition cmisDocumentStoreBeanDefinition;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(org.springframework.core.env.Environment)
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

		AnnotationAttributes attributes = new AnnotationAttributes(annotationMetadata.getAnnotationAttributes(EnableCmis.class.getName()));

		beanDefinitionRegistry.registerBeanDefinition(CMIS_REPOSITORY_INFO, createCmisRepositorInfoBeanDefinition(attributes, annotationMetadata));

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TypeDefinitionListImpl.class);
		ManagedList beanRefs = new ManagedList();
		builder.addPropertyValue("list", beanRefs);
		beanDefinitionRegistry.registerBeanDefinition(CMIS_TYPE_DEFINITION_LIST, builder.getBeanDefinition());

		String[] basePackages = attributes.getStringArray("basePackages");

		cmisDocumentScan(basePackages);
		cmisFolderScan(basePackages);

		// create cmis:document type definition
		Class<?> entityClass = null;
		Class<?> repoClass = null;
		Class<?> storeClass = null;
		try {
			entityClass = ClassUtils.forName(cmisDocumentBeanDefinition.getBeanClassName(), classLoader);
			repoClass = ClassUtils.forName(cmisDocumentRepositoryBeanDefinition.getBeanClassName(), classLoader);
			storeClass = ClassUtils.forName(cmisDocumentStoreBeanDefinition.getBeanClassName(), classLoader);

			String typeDefBeanName = format("%sTypeDefinition", cmisDocumentBeanDefinition.getBeanClassName());
			beanDefinitionRegistry.registerBeanDefinition(typeDefBeanName, createTypeDefinition(entityClass, repoClass, storeClass, annotationMetadata));

			// add type definition to the list that will be given to the typedefinitionlist bean
			beanRefs.add(new RuntimeBeanReference(typeDefBeanName));
		}
		catch (Throwable t) {
		}

		// create cmis:folder type definition
		try {
			entityClass = ClassUtils.forName(cmisFolderBeanDefinition.getBeanClassName(), classLoader);
			repoClass = ClassUtils.forName(cmisFolderRepositoryBeanDefinition.getBeanClassName(), classLoader);

			String typeDefBeanName = format("%sTypeDefinition", cmisFolderBeanDefinition.getBeanClassName());
			beanDefinitionRegistry.registerBeanDefinition(typeDefBeanName, createTypeDefinition(entityClass, repoClass, null, annotationMetadata));

			// add type definition to the list that will be given to the typedefinitionlist bean
			beanRefs.add(new RuntimeBeanReference(typeDefBeanName));
		}
		catch (Throwable t) {
		}

		// create the cmis repository configuration (the root configuration object)
		beanDefinitionRegistry.registerBeanDefinition("CmisRepositoryConfiguration", createCmisRepositoryConfigurationDefinition(cmisFolderRepositoryBeanDefinition, cmisDocumentRepositoryBeanDefinition, cmisDocumentStoreBeanDefinition, annotationMetadata));
	}

	void cmisDocumentScan(String... basePackages) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CmisDocument.class));

		for (String basePackage : basePackages) {
			for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {

				cmisDocumentBeanDefinition = bd;

				CmisEntityRepositoryComponentProvider cmisRepoScanner = new CmisEntityRepositoryComponentProvider(bd.getBeanClassName(), classLoader);
				scanner.setResourceLoader(resourceLoader);
				scanner.setEnvironment(environment);
				for (int i =0; i < basePackages.length && cmisDocumentRepositoryBeanDefinition == null; i++) {
					cmisDocumentRepositoryBeanDefinition = cmisRepoScanner.findCandidateComponents(basePackages[i])
							.stream()
							.findFirst()
							.get();
				}

				CmisEntityStorageComponentProvider cmisStoreScanner = new CmisEntityStorageComponentProvider(bd.getBeanClassName(), classLoader);
				scanner.setResourceLoader(resourceLoader);
				scanner.setEnvironment(environment);
				for (int i =0; i < basePackages.length && cmisDocumentStoreBeanDefinition == null; i++) {
					cmisDocumentStoreBeanDefinition = cmisStoreScanner.findCandidateComponents(basePackages[i])
						.stream()
						.findFirst()
						.get();
				}
			}
		}
	}

	void cmisFolderScan(String... basePackages) {

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CmisFolder.class));

		for (String basePackage : basePackages) {

			for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {

				if (bd.getBeanClassName().equals(CmisServiceBridge.Root.class.getName())) {
					continue;
				}

				cmisFolderBeanDefinition = bd;

				CmisEntityRepositoryComponentProvider cmisRepoScanner = new CmisEntityRepositoryComponentProvider(bd.getBeanClassName(), classLoader);
				scanner.setResourceLoader(resourceLoader);
				scanner.setEnvironment(environment);
				for (int i =0; i < basePackages.length && cmisFolderRepositoryBeanDefinition == null; i++) {
					cmisFolderRepositoryBeanDefinition = cmisRepoScanner.findCandidateComponents(basePackages[i])
							.stream()
							.findFirst()
							.get();
				}
			}
		}
	}

	BeanDefinition createCmisRepositorInfoBeanDefinition(AnnotationAttributes attributes, AnnotationMetadata annotationMetadata) {

		BeanDefinitionBuilder repoInfoBuilder = BeanDefinitionBuilder.genericBeanDefinition(RepositoryInfoImpl.class);
		repoInfoBuilder.getRawBeanDefinition().setSource(annotationMetadata);

		String id = attributes.getString("id");
		if (StringUtils.isEmpty(id)) {
			id = UUID.randomUUID().toString();
		}

		repoInfoBuilder.addPropertyValue("id", id);
		repoInfoBuilder.addPropertyValue("name", attributes.getString("name"));
		repoInfoBuilder.addPropertyValue("description", attributes.getString("description"));
		repoInfoBuilder.addPropertyValue("vendorName", attributes.getString("vendorName"));
		repoInfoBuilder.addPropertyValue("productName", attributes.getString("productName"));
		repoInfoBuilder.addPropertyValue("productVersion", attributes.getString("productVersion"));
		repoInfoBuilder.addPropertyValue("rootFolder", "@root@");

		return repoInfoBuilder.getBeanDefinition();
	}

	BeanDefinition createCmisRepositoryConfigurationDefinition(
			BeanDefinition cmisFolderRepoBeanDef,
			BeanDefinition cmisDocumentRepoBeanDef,
			BeanDefinition cmisDocumentStoreBeanDefinition,
			AnnotationMetadata annotationMetadata) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CmisRepositoryConfigurationImpl.class);
		builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		builder.getRawBeanDefinition().setSource(annotationMetadata);

		String beanName = new SpringDataAnnotationBeanNameGenerator().generateBeanName(cmisFolderRepoBeanDef);
		builder.addPropertyReference("cmisFolderRepository", beanName);

		beanName = new SpringDataAnnotationBeanNameGenerator().generateBeanName(cmisDocumentRepoBeanDef);
		builder.addPropertyReference("cmisDocumentRepository", beanName);

		beanName = StoreUtils.getStoreBeanName(cmisDocumentStoreBeanDefinition);
		builder.addPropertyReference("cmisDocumentStorage", beanName);

		builder.addPropertyReference("cmisRepositoryInfo", CMIS_REPOSITORY_INFO);

		builder.addPropertyReference("cmisTypeDefinitionList", CMIS_TYPE_DEFINITION_LIST);

		return builder.getBeanDefinition();
	}

	BeanDefinition createTypeDefinition(Class<?> bd, Class<?> rbd, Class<?> sbd, AnnotationMetadata annotationMetadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CmisTypeDefinitionFactoryBean.class);
		builder.getRawBeanDefinition().setSource(annotationMetadata);
		try {
			builder.addPropertyValue("entityClass", bd);
			builder.addPropertyValue("repoClass", rbd);
			builder.addPropertyValue("storeClass", sbd);
		}
		catch (Throwable t) {
		}
		return builder.getBeanDefinition();
	}

	static class CmisEntityRepositoryComponentProvider extends ClassPathScanningCandidateComponentProvider {

		public CmisEntityRepositoryComponentProvider(String cmisEntityClassName, ClassLoader classLoader) {
			AssignableTypeFilter assignableTypeFilter = new AssignableTypeFilter(Repository.class);
			CmisEntityTypeFilter cmisEntityTypeFilter = new CmisEntityTypeFilter(cmisEntityClassName, classLoader);

			this.addIncludeFilter(new AllTypeFilter(cmisEntityTypeFilter, assignableTypeFilter));
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return true;
		}
	}

	static class CmisEntityStorageComponentProvider extends ClassPathScanningCandidateComponentProvider {

		public CmisEntityStorageComponentProvider(String cmisEntityClassName, ClassLoader classLoader) {
			AssignableTypeFilter assignableTypeFilter = new AssignableTypeFilter(ContentStore.class);
			CmisEntityTypeFilter cmisEntityTypeFilter = new CmisEntityTypeFilter(cmisEntityClassName, classLoader);

			this.addIncludeFilter(new AllTypeFilter(cmisEntityTypeFilter, assignableTypeFilter));
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return true;
		}
	}

	static class AllTypeFilter implements TypeFilter {

		private final TypeFilter[] delegates;

		public AllTypeFilter(TypeFilter... delegates) {

			Assert.notNull(delegates, "TypeFilter delegates must not be null!");
			this.delegates = delegates;
		}

		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
				throws IOException {

			for (TypeFilter filter : delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}

			return true;
		}
	}

	static class CmisEntityTypeFilter extends AbstractTypeHierarchyTraversingFilter {

		private final String cmisEntityClassName;
		private final ClassLoader classLoader;

		protected CmisEntityTypeFilter(String cmisEntityClassName, ClassLoader classLoader) {
			super(false, false);
			Assert.hasLength(cmisEntityClassName, "cmisEntityClassName cant be null or empty");
			this.cmisEntityClassName = cmisEntityClassName;
			this.classLoader = classLoader;
		}

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
			return (hasCmisEntityType(metadataReader.getClassMetadata().getClassName()));
		}

		private boolean hasCmisEntityType(String interfaceName) {
			try {
				Class<?> iface = ClassUtils.forName(interfaceName, classLoader);
				String entityClassName = ((ParameterizedType)iface.getGenericInterfaces()[0]).getActualTypeArguments()[0].getTypeName();
				return (cmisEntityClassName.equals(entityClassName));
			}
			catch (Throwable t) {
				// no match
			}
			return false;
		}
	}
}
