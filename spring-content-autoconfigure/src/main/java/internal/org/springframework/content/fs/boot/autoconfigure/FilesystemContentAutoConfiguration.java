package internal.org.springframework.content.fs.boot.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import internal.org.springframework.content.fs.config.FilesystemStoreConfiguration;

import java.io.IOException;
import java.nio.file.Files;

@Configuration
@Import({FilesystemContentAutoConfigureRegistrar.class, FilesystemStoreConfiguration.class})
public class FilesystemContentAutoConfiguration {

	@ConditionalOnMissingBean(FileSystemResourceLoader.class)

    @Bean
	FileSystemResourceLoader fileSystemResourceLoader(FilesystemProperties properties) {
		return new FileSystemResourceLoader(properties.getFilesystemRoot());
	}

	@Component
	@ConfigurationProperties(prefix = "spring.content.fs", exceptionIfInvalid = true, ignoreUnknownFields = true)
	public static class FilesystemProperties {

	    private static final Logger logger = LoggerFactory.getLogger(FilesystemProperties.class);

		String filesystemRoot;

		public String getFilesystemRoot() {
            if (filesystemRoot == null) {
                try {
                    filesystemRoot = Files.createTempDirectory("").toString();
                } catch (IOException ioe) {
                    logger.error(String.format("Unexpected error defaulting filesystem root to %s", filesystemRoot), ioe);
                }
            }

		    return this.filesystemRoot;
		}

		public void setFilesystemRoot(String filesystemRoot) {
			this.filesystemRoot = filesystemRoot;
		}
	}
}
