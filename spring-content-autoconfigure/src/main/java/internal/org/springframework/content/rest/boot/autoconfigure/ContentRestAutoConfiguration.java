package internal.org.springframework.content.rest.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnWebApplication()
@ConditionalOnClass({RestConfiguration.class})
@Import(RestConfiguration.class)
public class ContentRestAutoConfiguration {
}
