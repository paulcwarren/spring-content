package internal.org.springframework.content.s3.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.s3.config.S3StoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class S3StoreConfiguration {
	
	@Autowired(required=false) private List<S3StoreConverter<?,String>> customConverters;

	@Bean
	public ConversionService s3StoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		if (customConverters != null) {
			for (Converter<?,String> converter : customConverters) {
				conversion.addConverter(converter);
			}
		}
		return conversion;
	}
	
}
