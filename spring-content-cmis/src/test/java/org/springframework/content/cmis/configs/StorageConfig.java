package org.springframework.content.cmis.configs;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFilesystemStores
public class StorageConfig {

	@Bean
	FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
		return new FileSystemResourceLoader(Files.createTempDirectory("").toString());
	}

}
