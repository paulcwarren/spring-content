package internal.org.springframework.content.azure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.azure.spring.autoconfigure.storage.resource.AzureStorageProtocolResolver;

@Configuration
@Import(AzureStorageProtocolResolver.class)
public class AzureStorageConfiguration {

	@Autowired(required = false)
	private List<AzureStorageConfigurer> configurers;

	@Value("${spring.content.azure.bucket:#{environment.AZURE_STORAGE_BUCKET}}")
	private String bucket;

	@Bean
	public PlacementService azureStoragePlacementService() {
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

		for (AzureStorageConfigurer configurer : configurers) {
			configurer.configureAzureStorageConverters(registry);
		}
	}
}
