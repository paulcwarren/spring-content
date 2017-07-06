package internal.org.springframework.content.fs.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.FilesystemStoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class FilesystemStoreConfiguration {

	@Autowired(required=false) private List<FilesystemStoreConfigurer> configurers;

	@Bean ConversionService filesystemStoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		addConverters(conversion);
		return conversion;
	}

	protected void addConverters(ConverterRegistry registry) {
		if (configurers == null) 
			return;
		
		for (FilesystemStoreConfigurer configurer : configurers) {
			configurer.configureFilesystemStoreConverters(registry);
		}
	}
}
