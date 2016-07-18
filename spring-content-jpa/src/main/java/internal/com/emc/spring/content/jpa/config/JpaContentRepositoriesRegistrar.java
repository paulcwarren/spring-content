package internal.com.emc.spring.content.jpa.config;

import java.lang.annotation.Annotation;

import com.emc.spring.content.commons.config.AbstractContentStoreBeanDefinitionRegistrar;
import com.emc.spring.content.jpa.config.EnableJpaContentRepositories;

public class JpaContentRepositoriesRegistrar extends AbstractContentStoreBeanDefinitionRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaContentRepositories.class;
	}

}
