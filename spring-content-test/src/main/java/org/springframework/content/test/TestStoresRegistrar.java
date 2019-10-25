package org.springframework.content.test;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;

public class TestStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableTestStores.class;
	}

	@Override
	protected Class<?>[] getIdentifyingTypes() {
		return new Class[]{TestContentStore.class};
	}
}
