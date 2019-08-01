package internal.org.springframework.content.s3.boot.autoconfigure;

import java.util.Set;

import internal.org.springframework.content.commons.utils.StoreUtils;
import internal.org.springframework.content.s3.config.S3StoresRegistrar;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

public class S3ContentAutoConfigureRegistrar extends S3StoresRegistrar {

	@Override
	protected void registerContentStoreBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(EnableS3ContentAutoConfiguration.class);
		AnnotationAttributes attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(this.getAnnotation().getName()));

		String[] basePackages = this.getBasePackages();

		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(this.getResourceLoader(), basePackages, multipleStoreImplementationsDetected(), getIdentifyingType());

		buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected String[] getBasePackages() {
		return AutoConfigurationPackages.get(this.getBeanFactory())
				.toArray(new String[] {});
	}

	@EnableS3Stores
	private static class EnableS3ContentAutoConfiguration {
		//
	}
}
