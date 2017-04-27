package internal.org.springframework.content.fs.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.FilesystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class FilesystemStoreConfiguration {

	@Autowired(required=false) private List<FilesystemStoreConverter<?,String>> customConverters;
	
	@Bean
	public FilesystemProperties filesystemProperties() {
		return new FilesystemProperties();
	}
	
	@Bean FileSystemResourceLoader fileSystemResourceLoader() {
		return new FileSystemResourceLoader(filesystemProperties().getFilesystemRoot());
	}
	
	@Bean ConversionService filesystemStoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		if (customConverters != null) {
			for (Converter<?,String> converter : customConverters) {
				conversion.addConverter(converter);
			}
		}
		return conversion;
	}
}
