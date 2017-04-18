package internal.org.springframework.content.mongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class MongoStoreConfiguration {

	@Bean
	public DefaultConversionService mongoStoreConverter() {
		return new DefaultConversionService();
	}
}
