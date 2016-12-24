package internal.org.springframework.content.mongo.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.mongo.config.EnableMongoContentRepositories;

import internal.org.springframework.content.mongo.operations.MongoContentTemplate;

public class MongoContentStoresRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoContentRepositories.class;
	}

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		String beanName = "mongoContentTemplate";
	    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MongoContentTemplate.class);
	    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}
}
