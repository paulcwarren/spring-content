package internal.org.springframework.content.gcs.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.google.cloud.spring.storage.GoogleStorageProtocolResolver;

@Configuration
@Import(GoogleStorageProtocolResolver.class)
public class GCPStorageConfiguration {

	@Autowired(required = false)
	private List<GCPStorageConfigurer> configurers;

	@Value("${spring.content.gcp.storage.bucket:#{environment.GCP_STORAGE_BUCKET}}")
	private String bucket;

	@Bean
	public PlacementService gcpStoragePlacementService() {
		PlacementService conversion = new PlacementServiceImpl();

		addDefaultConverters(conversion, bucket);
		addConverters(conversion);

		return conversion;
	}

	public static void addDefaultConverters(PlacementService conversion, String bucket) {

		conversion.addConverter(new BlobIdResolverConverter(bucket));
	}

	private void addConverters(ConverterRegistry registry) {
		if (configurers == null)
			return;

		for (GCPStorageConfigurer configurer : configurers) {
			configurer.configureGCPStorageConverters(registry);
		}
	}
}
