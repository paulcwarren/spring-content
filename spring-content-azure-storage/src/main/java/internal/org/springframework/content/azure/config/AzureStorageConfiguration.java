package internal.org.springframework.content.azure.config;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.azure.Bucket;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.azure.spring.autoconfigure.storage.resource.AzureStorageProtocolResolver;

@Configuration
@Import(AzureStorageProtocolResolver.class)
public class AzureStorageConfiguration implements InitializingBean {

	private static Log logger = LogFactory.getLog(AzureStorageConfiguration.class);

	@Autowired(required = false)
	private List<AzureStorageConfigurer> configurers;

	private String bucket;
	
	private PlacementService conversion = new PlacementServiceImpl();

	@Autowired
	public AzureStorageConfiguration(@Value("${spring.content.azure.bucket:#{environment.AZURE_STORAGE_BUCKET}}") String bucket) {
		this.bucket = bucket;
	}

	@Bean
	public PlacementService azureStoragePlacementService() {
		return conversion;
	}

	public static void addDefaultConverters(PlacementService conversion, String bucket) {

		// Serializable -> BlobId
		conversion.addConverter(new BlobIdResolverConverter(bucket));

		// ContentPropertyInfo -> BlobId
		conversion.addConverter(new Converter<ContentPropertyInfo<Object, Serializable>, BlobId>() {

			private String defaultBucket = bucket;

			@Override
			public BlobId convert(ContentPropertyInfo<Object, Serializable> info) {

				String strBucket = null;
				// @Bucket can be only on entity level, not per content property
				Object bucket = BeanUtils.getFieldWithAnnotation(info.entity(), Bucket.class);
				if (bucket == null) {
					bucket = defaultBucket;
				}
				if (bucket == null) {
					throw new StoreAccessException("Bucket not set");
				} else {
					strBucket = bucket.toString();
				}

				Serializable key = info.contentId();

				return (key != null) ? new BlobId(strBucket, key.toString()) : null;
			}

		});
	}

	private void addConverters(ConverterRegistry registry) {
		if (configurers == null)
			return;

		for (AzureStorageConfigurer configurer : configurers) {
			configurer.configureAzureStorageConverters(registry);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Configuring default converters");
		addDefaultConverters(conversion, bucket);
		addConverters(conversion);
	}
}
