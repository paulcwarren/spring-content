package internal.org.springframework.content.mongo.boot.autoconfigure;

import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

import internal.org.springframework.content.commons.utils.StoreCandidateComponentProvider;
import internal.org.springframework.content.commons.utils.StoreUtils;
import internal.org.springframework.content.mongo.config.MongoContentStoresRegistrar;

public class MongoContentAutoConfigureRegistrar extends MongoContentStoresRegistrar {

	@Override
	protected void registerContentStoreBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(
				EnableMongoContentAutoConfiguration.class);
		AnnotationAttributes attributes = new AnnotationAttributes(
				metadata.getAnnotationAttributes(this.getAnnotation().getName()));

		String[] basePackages = this.getBasePackages();

        StoreCandidateComponentProvider scanner = new StoreCandidateComponentProvider(false, this.getEnvironment());
        scanner.setResourceLoader(this.getResourceLoader());

        Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(
                scanner,
                this.getEnvironment(),
                this.getResourceLoader(),
                basePackages,
                multipleStoreImplementationsDetected(),
                this.getSignatureTypes(),
                this.getOverridePropertyValue());

		this.buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected String[] getBasePackages() {
		return AutoConfigurationPackages.get(this.getBeanFactory())
				.toArray(new String[] {});
	}

	@EnableMongoStores
	private static class EnableMongoContentAutoConfiguration {
	}

}
