package internal.org.springframework.content.rest.boot.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@AutoConfiguration
@ConditionalOnWebApplication()
@ConditionalOnClass({ RestConfiguration.class })
@Import(RestConfiguration.class)
public class ContentRestAutoConfiguration {

	@Component
	@ConfigurationProperties(prefix="spring.content.rest")
	public static class ContentRestProperties {

		private URI baseUri;
		private boolean fullyQualifiedLinks = RestConfiguration.FULLY_QUALIFIED_DEFAULTS_DEFAULT;
		private ShortcutRequestMappings requestMappings = new ShortcutRequestMappings();
		private boolean overwriteExistingContent = RestConfiguration.OVERWRITE_EXISTING_CONTENT_DEFAULT;

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

		public ShortcutRequestMappings shortcutRequestMappings() {
		    return this.requestMappings;
		}

		public void setShortcutRequestMappings(ShortcutRequestMappings requestMappings) {
		    this.requestMappings = requestMappings;
		}

		public boolean getOverwriteExistingContent() {
			return this.overwriteExistingContent;
		}

		public void setOverwriteExistingContent(boolean overwriteExistingContent) {
			this.overwriteExistingContent = overwriteExistingContent;
		}

		public static class ShortcutRequestMappings {

		    private boolean disabled = false;
		    private String excludes = null;

            public boolean disabled() {
                return this.disabled;
            }

            public void setDisabled(boolean disabled) {
                this.disabled = disabled;
            }

		    public String excludes() {
		        return excludes;
		    }

		    public void setExcludes(String excludes) {
		        this.excludes = excludes;
		    }
		}
    }

	@Bean
	public SpringBootContentRestConfigurer springBootContentRestConfigurer() {
		return new SpringBootContentRestConfigurer();
	}

}
