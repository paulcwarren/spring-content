package internal.org.springframework.content.s3.config;

import java.lang.annotation.Annotation;

import org.springframework.content.s3.config.EnableS3ContentRepositories;

public class S3ContentRepositoriesRegistrar extends S3StoresRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableS3ContentRepositories.class;
	}
}
