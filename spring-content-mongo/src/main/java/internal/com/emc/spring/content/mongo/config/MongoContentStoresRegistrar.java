package internal.com.emc.spring.content.mongo.config;

import java.lang.annotation.Annotation;

import com.emc.spring.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import com.emc.spring.content.mongo.config.EnableMongoContentRepositories;

public class MongoContentStoresRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoContentRepositories.class;
	}
}
