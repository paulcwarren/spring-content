package internal.org.springframework.content.jpa.config;

import java.lang.annotation.Annotation;

import org.springframework.content.common.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.jpa.config.EnableFilesystemContentRepositories;

public class FilesystemContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}

}
