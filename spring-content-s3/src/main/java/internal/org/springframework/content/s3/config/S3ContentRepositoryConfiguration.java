package internal.org.springframework.content.s3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class S3ContentRepositoryConfiguration {
	
	@Bean
	public ConversionService s3StoreConverter() {
		return new DefaultConversionService();
	}
	
}
