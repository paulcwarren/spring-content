package internal.org.springframework.content.rest.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

import internal.org.springframework.content.rest.config.ContentRestConfiguration;

@Configuration
@ConditionalOnClass(DispatcherServlet.class)
@Import(ContentRestConfiguration.class)
public class ContentRestAutoConfiguration {

}
