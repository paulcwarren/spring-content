package internal.org.springframework.content.config;

import java.lang.annotation.Annotation;

import org.springframework.content.common.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.config.EnableMongoContentStores;

public class MongoContentStoresRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoContentStores.class;
	}
}
