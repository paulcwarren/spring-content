package internal.org.springframework.content.rest.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

@Configuration
@ConditionalOnClass(HypermediaConfiguration.class)
@ConditionalOnBean(RepositoryRestMvcConfiguration.class)
@AutoConfigureAfter({ RepositoryRestMvcAutoConfiguration.class })
@Import(HypermediaConfiguration.class)
public class HypermediaAutoConfiguration {
	//
}
