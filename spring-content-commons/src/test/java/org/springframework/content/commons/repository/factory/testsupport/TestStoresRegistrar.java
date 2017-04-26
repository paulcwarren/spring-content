package org.springframework.content.commons.repository.factory.testsupport;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;

public class TestStoresRegistrar
		extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableTestStores.class;
	}

}
