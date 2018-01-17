package internal.org.springframework.content.jpa.config;

import java.lang.annotation.Annotation;

import internal.org.springframework.content.commons.storeservice.ContentStoreServiceImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;
import org.springframework.content.jpa.config.EnableJpaContentRepositories;
import org.springframework.content.jpa.config.EnableJpaStores;

import internal.org.springframework.content.jpa.operations.JpaContentTemplate;
import org.springframework.core.type.AnnotationMetadata;

public class JpaStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		String beanName = "jpaContentTemplate";
	    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JpaContentTemplate.class);
	    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaStores.class;
	}

}
