package internal.org.springframework.content.jpa.boot.autoconfigure;

import java.util.Set;

import internal.org.springframework.content.commons.utils.StoreUtils;
import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

public class JpaContentAutoConfigureRegistrar extends JpaStoresRegistrar {

	@Override
	protected void registerContentStoreBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(
				EnableJpaContentAutoConfiguration.class);
		AnnotationAttributes attributes = new AnnotationAttributes(
				metadata.getAnnotationAttributes(this.getAnnotation().getName()));

		String[] basePackages = this.getBasePackages();

		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(this.getEnvironment(), this.getResourceLoader(), basePackages, multipleStoreImplementationsDetected(), getIdentifyingTypes());

		this.buildAndRegisterDefinitions(importingClassMetadata, registry, attributes, basePackages, definitions);
	}

	protected String[] getBasePackages() {
		return AutoConfigurationPackages.get(this.getBeanFactory())
				.toArray(new String[] {});
	}

	@EnableJpaStores
	private static class EnableJpaContentAutoConfiguration {
	}
}
