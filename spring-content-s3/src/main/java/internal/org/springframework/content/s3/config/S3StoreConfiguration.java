package internal.org.springframework.content.s3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.content.s3.config.S3ObjectIdResolvers;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.ConverterRegistry;

import java.util.List;

@Configuration
public class S3StoreConfiguration {

	@Autowired(required = false)
	private List<S3StoreConfigurer> configurers;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;

	@Bean
	public S3ObjectIdResolvers contentIdResolvers() {
		S3ObjectIdResolvers resolvers = new S3ObjectIdResolvers();
		if (configurers != null) {
			for (S3StoreConfigurer configurer : configurers) {
				configurer.configureS3ObjectIdResolvers(resolvers);
			}
		}
		return resolvers;
	}

	@Bean
	public PlacementService s3StorePlacementService() {
		PlacementService conversion = new PlacementServiceImpl();

		for (S3ObjectIdResolver resolver : contentIdResolvers()) {
			conversion.addConverter(new S3ObjectIdResolverConverter(resolver, bucket));
		}

		addDefaultS3ObjectIdConverters(conversion, bucket);

		addConverters(conversion);
		return conversion;
	}

	public static void addDefaultS3ObjectIdConverters(PlacementService conversion, String bucket) {
		// Serializable -> S3ObjectId
		conversion.addConverter(new S3ObjectIdResolverConverter(S3StoreFactoryBean.DEFAULT_S3OBJECTID_RESOLVER_STORE, bucket));
		// Object -> S3ObjectId
		conversion.addConverter(new S3ObjectIdResolverConverter(new DefaultAssociativeStoreS3ObjectIdResolver(), bucket));
	}

	private void addConverters(ConverterRegistry registry) {
		if (configurers == null)
			return;
		for (S3StoreConfigurer configurer : configurers) {
			configurer.configureS3StoreConverters(registry);
		}
	}
}
