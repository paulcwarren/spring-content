package internal.org.springframework.content.commons.utils;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

public class ContentRepositoryUtils {

	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String CONTENT_REPOSITORY_FACTORY_BEAN_CLASS = "contentRepositoryFactoryBeanClass";
	
	public static String[] getBasePackages(AnnotationAttributes attributes, String[] defaultPackages) {
		
		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {
			return defaultPackages;
		}

		Set<String> packages = new HashSet<String>();
		packages.addAll(Arrays.asList(value));
		packages.addAll(Arrays.asList(basePackages));

		for (Class<?> typeName : basePackageClasses) {
			packages.add(ClassUtils.getPackageName(typeName));
		}

		return packages.toArray(new String[] {});
	}

	public static String getRepositoryFactoryBeanName(AnnotationAttributes attributes) {
		return attributes.getClass(CONTENT_REPOSITORY_FACTORY_BEAN_CLASS).getName();
	}
	
	public static String getRepositoryBeanName(BeanDefinition definition) {
		String beanName = ClassUtils.getShortName(definition.getBeanClassName());
		return Introspector.decapitalize(beanName);
	}
	
	public static Set<GenericBeanDefinition> getContentRepositoryCandidates(ResourceLoader loader, String[] basePackages) {
		ContentRepositoryCandidateComponentProvider scanner = new ContentRepositoryCandidateComponentProvider(false);
		//scanner.setConsiderNestedRepositoryInterfaces(shouldConsiderNestedRepositories());
		scanner.setResourceLoader(loader);
		//scanner.setEnvironment(environment);

		/*for (TypeFilter filter : getExcludeFilters()) {
			scanner.addExcludeFilter(filter);
		}*/

		Set<GenericBeanDefinition> result = new HashSet<>();

		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates)
				result.add((GenericBeanDefinition)candidate);
		}

		return result;	
	}
}
