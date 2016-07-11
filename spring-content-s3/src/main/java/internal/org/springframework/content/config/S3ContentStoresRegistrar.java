package internal.org.springframework.content.config;

import java.lang.annotation.Annotation;

import org.springframework.content.common.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.config.EnableS3ContentStores;

public class S3ContentStoresRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableS3ContentStores.class;
	}
}
