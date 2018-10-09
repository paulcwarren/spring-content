package internal.org.springframework.content.gcs.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.gcs.config.EnableGCSContentRepositories;

public class GCSContentRepositoriesRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableGCSContentRepositories.class;
	}
}
