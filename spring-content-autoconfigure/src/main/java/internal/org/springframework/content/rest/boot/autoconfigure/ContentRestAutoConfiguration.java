package internal.org.springframework.content.rest.boot.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Configuration
@ConditionalOnWebApplication()
@ConditionalOnClass({ RestConfiguration.class })
@Import(RestConfiguration.class)
public class ContentRestAutoConfiguration {

	@Component
	@ConfigurationProperties(prefix="spring.content.rest")
	public static class ContentRestProperties {

		private URI baseUri;
		private boolean fullyQualifiedLinks = false;

		public URI getBaseUri() {
			return baseUri;
		}

		public void setBaseUri(URI baseUri) {
			this.baseUri = baseUri;
		}

		public boolean fullyQualifiedLinks() {
			return this.fullyQualifiedLinks;
		}

		public void setFullyQualifiedLinks(boolean fullyQualifiedLinks) {
			this.fullyQualifiedLinks = fullyQualifiedLinks;
		}
    }

	@Bean
	public SpringBootContentRestConfigurer springBootContentRestConfigurer() {
		return new SpringBootContentRestConfigurer();
	}

}
