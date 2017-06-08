package internal.org.springframework.content.fs.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import internal.org.springframework.content.fs.config.FilesystemStoreConfiguration;

@Configuration
@Import({FilesystemContentAutoConfigureRegistrar.class, FilesystemStoreConfiguration.class})
public class FilesystemContentAutoConfiguration {

	@Component
	@ConfigurationProperties(prefix = "spring.content.fs", exceptionIfInvalid = true, ignoreUnknownFields = true)
	public static class FilesystemContentProperties {

		String filesystemRoot;

		public String getFilesystemRoot() {
			return this.filesystemRoot;
		}

		public void setFilesystemRoot(String filesystemRoot) {
			this.filesystemRoot = filesystemRoot;
		}
	}
}
