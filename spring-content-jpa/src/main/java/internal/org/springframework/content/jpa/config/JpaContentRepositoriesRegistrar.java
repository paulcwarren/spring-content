package internal.org.springframework.content.jpa.config;

import org.springframework.content.jpa.config.EnableJpaContentRepositories;

import java.lang.annotation.Annotation;

public class JpaContentRepositoriesRegistrar extends JpaStoresRegistrar {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaContentRepositories.class;
	}

}
