package internal.org.springframework.content.fs.config;

import java.lang.annotation.Annotation;

import org.springframework.content.fs.config.EnableFilesystemContentRepositories;

@SuppressWarnings("deprecation")
public class FilesystemContentRepositoriesRegistrar extends FilesystemStoreRegistrar {

	public FilesystemContentRepositoriesRegistrar() {
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFilesystemContentRepositories.class;
	}
	
}
