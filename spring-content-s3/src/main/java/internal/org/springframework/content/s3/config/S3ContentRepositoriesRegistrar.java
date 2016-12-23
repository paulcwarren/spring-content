package internal.org.springframework.content.s3.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.s3.config.EnableS3ContentRepositories;

import internal.org.springframework.content.s3.operations.S3ResourceTemplate;

public class S3ContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		String beanName = "s3ResourceTemplate";
	    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(S3ResourceTemplate.class);
	    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableS3ContentRepositories.class;
	}
}
