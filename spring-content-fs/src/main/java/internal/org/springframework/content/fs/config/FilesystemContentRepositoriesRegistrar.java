package internal.org.springframework.content.fs.config;

import java.lang.annotation.Annotation;

import org.springframework.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;

public class FilesystemContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}

}
