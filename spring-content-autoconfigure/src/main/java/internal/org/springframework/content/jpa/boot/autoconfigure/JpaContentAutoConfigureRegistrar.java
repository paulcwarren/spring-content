package internal.org.springframework.content.jpa.boot.autoconfigure;

import internal.org.springframework.content.commons.utils.StoreUtils;
import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

import java.util.Set;

public class JpaContentAutoConfigureRegistrar extends JpaStoresRegistrar {

	@Override
	protected void registerContentStoreBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(EnableJpaContentAutoConfiguration.class);
		AnnotationAttributes attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(this.getAnnotation().getName()));

		String[] basePackages = this.getBasePackages();

		Set<GenericBeanDefinition> definitions = StoreUtils.getStoreCandidates(this.getResourceLoader(), basePackages);

		for (BeanDefinition definition : definitions) {

			String factoryBeanName = StoreUtils.getStoreFactoryBeanName(attributes);

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

			builder.getRawBeanDefinition().setSource(importingClassMetadata);
			builder.addPropertyValue(AbstractStoreBeanDefinitionRegistrar.STORE_INTERFACE_PROPERTY, definition.getBeanClassName());

			registry.registerBeanDefinition(StoreUtils.getStoreBeanName(definition), builder.getBeanDefinition());
		}
	}

	protected String[] getBasePackages() {
		return AutoConfigurationPackages.get(this.getBeanFactory()).toArray(new String[] {});
	}

	@EnableJpaStores
	private static class EnableJpaContentAutoConfiguration {
	}
}
