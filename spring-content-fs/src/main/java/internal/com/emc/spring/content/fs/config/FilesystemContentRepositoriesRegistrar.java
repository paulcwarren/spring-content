package internal.com.emc.spring.content.fs.config;

import java.lang.annotation.Annotation;

import com.emc.spring.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import com.emc.spring.content.fs.config.EnableFilesystemContentRepositories;

public class FilesystemContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}

}
