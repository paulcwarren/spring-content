package internal.org.springframework.content.mongo.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class MongoStoreConfiguration {

	@Autowired(required=false) private List<MongoStoreConverter<?,String>> customConverters;

	@Bean
	public DefaultConversionService mongoStoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		if (customConverters != null) {
			for (Converter<?,String> converter : customConverters) {
				conversion.addConverter(converter);
			}
		}
		return conversion;
	}
}
