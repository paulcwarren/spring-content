package internal.org.springframework.boot.autoconfigure.content.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;


@Configuration
@ConditionalOnClass(DispatcherServlet.class)
@ComponentScan(basePackages = {"internal.org.springframework.content"})
public class ContentRestAutoConfiguration {

}
