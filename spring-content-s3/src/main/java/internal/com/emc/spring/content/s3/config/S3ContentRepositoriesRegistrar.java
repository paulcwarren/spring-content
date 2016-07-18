package internal.com.emc.spring.content.s3.config;

import java.lang.annotation.Annotation;

import com.emc.spring.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import com.emc.spring.content.s3.config.EnableS3ContentRepositories;

public class S3ContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableS3ContentRepositories.class;
	}
}
