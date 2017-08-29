package internal.org.springframework.content.jpa.boot.autoconfigure;

import javax.persistence.EntityManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;

@Configuration
@ConditionalOnClass({EntityManager.class, JpaStoresRegistrar.class})
@Import(JpaContentAutoConfigureRegistrar.class)
public class JpaContentAutoConfiguration {

}
