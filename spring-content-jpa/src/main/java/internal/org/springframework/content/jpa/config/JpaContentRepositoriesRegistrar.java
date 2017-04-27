package internal.org.springframework.content.jpa.config;

import java.lang.annotation.Annotation;

import org.springframework.content.jpa.config.EnableJpaContentRepositories;

public class JpaContentRepositoriesRegistrar extends JpaStoresRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaContentRepositories.class;
	}

}
