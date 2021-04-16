package internal.org.springframework.content.commons.utils;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class StoreUtils {

    private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String STORE_FACTORY_BEAN_CLASS = "storeFactoryBeanClass";
	private static final String SPRING_CONTENT_STORAGE_TYPE_DEFAULT = "spring.content.storage.type.default";

	public static String[] getBasePackages(AnnotationAttributes attributes, String[] defaultPackages) {

		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0
				&& basePackageClasses.length == 0) {
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

    public static Set<GenericBeanDefinition> getStoreCandidates(StoreCandidateComponentProvider scanner, Environment env, ResourceLoader loader, String[] basePackages, boolean multiStoreMode, Class<?>[] signatureTypes, String registrarOverridePropertyValue) {

        Set<GenericBeanDefinition> result = new HashSet<>();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {

                boolean qualifiedForImplementation = !multiStoreMode ||
                        candidateImplementsSignatureType(signatureTypes, candidate.getBeanClassName(), loader) ||
                        registrarMatchesOverrideProperty(env.getProperty(SPRING_CONTENT_STORAGE_TYPE_DEFAULT), registrarOverridePropertyValue);
                if (qualifiedForImplementation) {
                    result.add((GenericBeanDefinition)candidate);
                }
            }
        }

        return result;
    }

    protected static boolean candidateImplementsSignatureType(Class<?>[] identifyingTypes, String storeInterface, ResourceLoader loader) {

        Class<?> storeClass = null;
        try {
            storeClass = loader.getClassLoader().loadClass(storeInterface);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (Class<?> identifyingType : identifyingTypes) {
            if (identifyingType.isAssignableFrom(storeClass)) {
                return true;
            }
        }

        return false;
    }

    private static boolean registrarMatchesOverrideProperty(String overrideProperty, String registrarOverridePropertyValue) {

	    if (!StringUtils.hasLength(overrideProperty)) {
	        return false;
	    }
	    return overrideProperty.equals(registrarOverridePropertyValue);
    }
}
