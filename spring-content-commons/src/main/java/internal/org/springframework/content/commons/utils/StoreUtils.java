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

public class StoreUtils {

	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String STORE_FACTORY_BEAN_CLASS = "storeFactoryBeanClass";
	
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

	public static String getStoreFactoryBeanName(AnnotationAttributes attributes) {
		return attributes.getClass(STORE_FACTORY_BEAN_CLASS).getName();
	}
	
	public static String getStoreBeanName(BeanDefinition definition) {
		String beanName = ClassUtils.getShortName(definition.getBeanClassName());
		return Introspector.decapitalize(beanName);
	}
	
	public static Set<GenericBeanDefinition> getStoreCandidates(ResourceLoader loader, String[] basePackages) {
		StoreCandidateComponentProvider scanner = new StoreCandidateComponentProvider(false);
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
