package internal.org.springframework.content.gcs.config;

import java.io.Serializable;
import java.util.List;

import com.google.cloud.storage.BlobId;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.gcs.Bucket;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.google.cloud.spring.storage.GoogleStorageProtocolResolver;

@Configuration
@Import(GoogleStorageProtocolResolver.class)
public class GCPStorageConfiguration implements InitializingBean {

	@Autowired(required = false)
	private List<GCPStorageConfigurer> configurers;

	@Value("${spring.content.gcp.storage.bucket:#{environment.GCP_STORAGE_BUCKET}}")
	private String bucket;

	private PlacementService conversion = new PlacementServiceImpl();

	@Bean
	public PlacementService gcpStoragePlacementService() {
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

				return (key != null) ? BlobId.of(strBucket, key.toString()) : null;
			}

		});
	}

	private void addConverters(ConverterRegistry registry) {
		if (configurers == null)
			return;

		for (GCPStorageConfigurer configurer : configurers) {
			configurer.configureGCPStorageConverters(registry);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addDefaultConverters(conversion, bucket);
		addConverters(conversion);
	}
}
