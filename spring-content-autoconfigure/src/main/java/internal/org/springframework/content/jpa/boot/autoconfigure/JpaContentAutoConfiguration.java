package internal.org.springframework.content.jpa.boot.autoconfigure;

import javax.persistence.EntityManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(EntityManager.class)
@Import(JpaContentAutoConfigureRegistrar.class)
public class JpaContentAutoConfiguration {

}
