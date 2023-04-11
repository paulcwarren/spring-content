package internal.org.springframework.content.fs.boot.autoconfigure;

import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import internal.org.springframework.content.fs.config.FilesystemStoreConfiguration;
import internal.org.springframework.content.fs.config.FilesystemStoreFactoryBean;
import internal.org.springframework.content.fs.config.FilesystemStoreRegistrar;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;

@AutoConfiguration
@AutoConfigureAfter({JpaVersionsAutoConfiguration.class})
@ConditionalOnClass(FilesystemStoreRegistrar.class)
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "fs",
        matchIfMissing=true)
public class FilesystemContentAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(FilesystemStoreFactoryBean.class)
	@Import({ FilesystemContentAutoConfigureRegistrar.class, FilesystemStoreConfiguration.class })
	public static class EnableFilesystemStoresConfig {
	}

	@Bean
	@ConditionalOnMissingBean(FileSystemResourceLoader.class)
	FileSystemResourceLoader fileSystemResourceLoader(FilesystemProperties props) {
		return new FileSystemResourceLoader(props.getFilesystemRoot());
	}

	@Component
	@ConfigurationProperties(prefix = "spring.content.fs")
	public static class FilesystemProperties {

		private static final Logger logger = LoggerFactory.getLogger(FilesystemProperties.class);

		/**
		 * The root location where file system stores place their content
		 */
		String filesystemRoot;

		public String getFilesystemRoot() {
			if (filesystemRoot == null) {
				try {
					filesystemRoot = Files.createTempDirectory("").toString();
				}
				catch (IOException ioe) {
					logger.error(String.format(
							"Unexpected error defaulting filesystem root to %s",
							filesystemRoot), ioe);
				}
			}

			return this.filesystemRoot;
		}

		public void setFilesystemRoot(String filesystemRoot) {
			this.filesystemRoot = filesystemRoot;
		}
	}
}
