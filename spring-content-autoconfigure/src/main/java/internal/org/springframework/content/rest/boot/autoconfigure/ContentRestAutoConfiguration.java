package internal.org.springframework.content.rest.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.net.URI;

@Configuration
@ConditionalOnWebApplication()
@ConditionalOnClass({ RestConfiguration.class })
@Import(RestConfiguration.class)
public class ContentRestAutoConfiguration {

	@Component
	@ConfigurationProperties(prefix="spring.content.rest")
	public static class ContentRestProperties {

		private URI baseUri;

		public URI getBaseUri() {
			return baseUri;
		}

		public void setBaseUri(URI baseUri) {
			this.baseUri = baseUri;
		}
	}
}
