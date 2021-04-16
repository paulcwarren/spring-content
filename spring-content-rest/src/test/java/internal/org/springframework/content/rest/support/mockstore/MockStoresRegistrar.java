package internal.org.springframework.content.rest.support.mockstore;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;

import java.lang.annotation.Annotation;

public class MockStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMockStores.class;
	}

	@Override
	protected Class<?>[] getSignatureTypes() {
		return new Class[]{MockContentStore.class};
	}
}
