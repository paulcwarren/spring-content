package internal.org.springframework.content.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@Configuration
public class MongoStoreConfiguration {

	@Autowired(required = false)
	private List<MongoStoreConverter<?, String>> customConverters;

	@Bean
	public PlacementService mongoStorePlacementService() {
		PlacementService conversion = new PlacementServiceImpl();

		if (customConverters != null) {
			for (Converter<?, String> converter : customConverters) {
				conversion.addConverter(converter);
			}
		}
		return conversion;
	}
}
