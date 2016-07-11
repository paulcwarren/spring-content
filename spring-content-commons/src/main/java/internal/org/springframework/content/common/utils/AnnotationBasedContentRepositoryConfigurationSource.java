package internal.org.springframework.content.common.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.content.common.config.ContentRepositoriesConfigurationSource;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class AnnotationBasedContentRepositoryConfigurationSource implements ContentRepositoriesConfigurationSource {

	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String CONTENT_REPOSITORY_FACTORY_BEAN_CLASS = "contentRepositoryFactoryBeanClass";
	
	private final AnnotationMetadata metadata;
	private final AnnotationAttributes attributes;
	
	public AnnotationBasedContentRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation) {
		
		//super(environment);

		Assert.notNull(metadata);
		Assert.notNull(annotation);

		this.attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(annotation.getName()));
		this.metadata = metadata;
	}
	
	public Iterable<String> getBasePackages() {
		
		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {
			String className = metadata.getClassName();
			return Collections.singleton(ClassUtils.getPackageName(className));
		}

		Set<String> packages = new HashSet<String>();
		packages.addAll(Arrays.asList(value));
		packages.addAll(Arrays.asList(basePackages));

		for (Class<?> typeName : basePackageClasses) {
			packages.add(ClassUtils.getPackageName(typeName));
		}

		return packages;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return attributes.getClass(CONTENT_REPOSITORY_FACTORY_BEAN_CLASS).getName();
	}


	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getSource()
	 */
	public Object getSource() {
		return metadata;
	}
}
