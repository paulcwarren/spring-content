package org.springframework.content.commons.repository.factory.testsupport;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;

public class TestStoresRegistrar
		extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableTestStores.class;
	}

}
