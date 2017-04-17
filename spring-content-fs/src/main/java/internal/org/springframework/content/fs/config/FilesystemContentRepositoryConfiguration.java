package internal.org.springframework.content.fs.config;

import java.util.UUID;

import org.springframework.content.commons.placement.PlacementStrategy;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import internal.org.springframework.content.commons.placement.StringBasedUUIDPlacementStrategy;
import internal.org.springframework.content.commons.placement.UUIDPlacementStrategy;

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
	
	@Bean
	public PlacementStrategy<String> stringUUIDPlacement() {
		return new StringBasedUUIDPlacementStrategy();
	}
	
	@Bean ConversionService filesystemStoreConverter() {
		return new DefaultConversionService();
	}
}
