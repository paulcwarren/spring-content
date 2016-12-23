package internal.org.springframework.content.fs.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;

public class FilesystemContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
		String beanName = "fileResourceTemplate";
	    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FileResourceTemplate.class);
	    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}

}
