package internal.org.springframework.content.gcs.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.gcs.config.GCSObjectIdResolvers;
import org.springframework.content.gcs.config.GCSStoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
public class GCSStoreConfiguration {

	@Autowired(required = false)
	private List<GCSStoreConfigurer> configurers;

	@Bean
	public GCSObjectIdResolvers contentIdResolvers() {
		GCSObjectIdResolvers resolvers = new GCSObjectIdResolvers();
		if (configurers != null) {
			for (GCSStoreConfigurer configurer : configurers) {
				configurer.configureGCSObjectIdResolvers(resolvers);
			}
		}
		return resolvers;
	}

	@Bean
	public ConversionService GCSStoreConverter() {
		DefaultConversionService conversion = new DefaultConversionService();
		addConverters(conversion);
		return conversion;
	}

	private void addConverters(DefaultConversionService conversion) {
		if (configurers == null)
			return;
		for (GCSStoreConfigurer configurer : configurers) {
			configurer.configureGCSStoreConverters(conversion);
		}
	}

}
