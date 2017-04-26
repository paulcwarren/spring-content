package internal.org.springframework.content.mongo.config;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.mongo.config.EnableMongoContentRepositories;

public class MongoContentStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoContentRepositories.class;
	}
}
