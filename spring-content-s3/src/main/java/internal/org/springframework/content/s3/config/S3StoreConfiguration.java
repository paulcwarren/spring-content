package internal.org.springframework.content.s3.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class S3StoreConfiguration {
	
	@Autowired(required=false) private List<S3StoreConfigurer> configurers;

	@Bean
	public ConversionService s3StoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		addConverters(conversion);
		return conversion;
	}

	private void addConverters(DefaultConversionService conversion) {
		if (configurers == null)
			return;
		for (S3StoreConfigurer configurer : configurers) {
			configurer.configureS3StoreConverters(conversion);
		}
	}
}
