package internal.com.emc.spring.content.rest.boot.autoconfigure;

import internal.com.emc.spring.content.rest.config.ContentRestConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
@ConditionalOnClass(DispatcherServlet.class)
@Import(ContentRestConfiguration.class)
public class ContentRestAutoConfiguration {

}
