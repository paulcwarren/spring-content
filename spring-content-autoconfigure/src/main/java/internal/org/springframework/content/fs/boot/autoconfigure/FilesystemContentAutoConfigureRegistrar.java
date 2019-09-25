package internal.org.springframework.content.fs.boot.autoconfigure;

import java.util.Set;

import internal.org.springframework.content.commons.utils.StoreUtils;
import internal.org.springframework.content.fs.config.FilesystemStoreRegistrar;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

public class FilesystemContentAutoConfigureRegistrar extends FilesystemStoreRegistrar {

	@Override
	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(EnableFilesystemContentAutoConfiguration.class);
		AnnotationAttributes attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(this.getAnnotation().getName()));

		String[] basePackages = this.getBasePackages();

		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(this.getEnvironment(), this.getResourceLoader(), basePackages, multipleStoreImplementationsDetected(), this.getIdentifyingTypes());

		this.buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected String[] getBasePackages() {
		return AutoConfigurationPackages.get(this.getBeanFactory())
				.toArray(new String[] {});
	}

	@EnableFilesystemStores
	private static class EnableFilesystemContentAutoConfiguration {
	}
}
