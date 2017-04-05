package internal.org.springframework.content.fs.config;

import java.util.UUID;

import org.springframework.content.commons.placementstrategy.PlacementStrategy;
import org.springframework.content.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import internal.org.springframework.content.commons.placementstrategy.UUIDPlacementStrategy;

@Configuration
public class FilesystemContentRepositoryConfiguration {

	@Bean
	public FilesystemProperties filesystemProperties() {
		return new FilesystemProperties();
	}
	
	@Bean FileSystemResourceLoader fileSystemResourceLoader() {
		return new FileSystemResourceLoader(filesystemProperties().getFilesystemRoot());
	}
	
	@Bean
	public PlacementStrategy<UUID> uuidPlacement() {
		return new UUIDPlacementStrategy();
	}
}
