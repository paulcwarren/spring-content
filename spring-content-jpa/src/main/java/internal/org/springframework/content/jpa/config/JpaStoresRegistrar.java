package internal.org.springframework.content.jpa.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;

public class JpaStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaStores.class;
	}

	@Override
	protected Class<?> getIdentifyingType() {
		return JpaContentStore.class;
	}
}