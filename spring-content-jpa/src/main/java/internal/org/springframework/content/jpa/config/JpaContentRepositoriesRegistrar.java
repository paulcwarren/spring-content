package internal.org.springframework.content.jpa.config;

import java.lang.annotation.Annotation;

import org.springframework.content.common.config.AbstractContentStoreBeanDefinitionRegistrar;
import org.springframework.content.jpa.config.EnableJpaContentRepositories;

public class JpaContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaContentRepositories.class;
	}

}
