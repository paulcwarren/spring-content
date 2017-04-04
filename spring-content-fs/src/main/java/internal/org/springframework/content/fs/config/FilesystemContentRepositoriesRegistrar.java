package internal.org.springframework.content.fs.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;
import org.springframework.core.type.AnnotationMetadata;

public class FilesystemContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		super.registerBeanDefinitions(importingClassMetadata, registry);
		
//		filesystemProperties(registry);
	}

	@Override
	protected void createOperationsBean(BeanDefinitionRegistry registry) {
//		String beanName = "fileResourceTemplate";
//	    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FileResourceTemplate.class);
//	    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}
	
	/* package */ void filesystemProperties(BeanDefinitionRegistry registry) {
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClass(FilesystemProperties.class);

		MutablePropertyValues values = new MutablePropertyValues();
		beanDef.setPropertyValues(values);
		
		registry.registerBeanDefinition("filesystemProperties", beanDef);
	}
}
