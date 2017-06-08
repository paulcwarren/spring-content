package internal.org.springframework.content.fs.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.core.type.AnnotationMetadata;

public class FilesystemStoreRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		super.registerBeanDefinitions(importingClassMetadata, registry);
	}

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemStores.class;
	}
	
}
