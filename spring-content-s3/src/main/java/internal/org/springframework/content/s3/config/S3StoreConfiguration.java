package internal.org.springframework.content.s3.config;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.ContentPropertyInfo;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

@Configuration
public class S3StoreConfiguration {

	@Autowired(required = false)
	private List<S3StoreConfigurer> configurers;

	@Value("${spring.content.s3.bucket:#{environment.AWS_BUCKET}}")
	private String bucket;

	@Bean
	public PlacementService s3StorePlacementService() {
		PlacementService conversion = new PlacementServiceImpl();

		addDefaultS3ObjectIdConverters(conversion, bucket);
		addConverters(conversion);

		return conversion;
	}

	public static void addDefaultS3ObjectIdConverters(PlacementService conversion, String bucket) {
		// Serializable -> S3ObjectId
		conversion.addConverter(new Converter<Serializable, S3ObjectId>() {

		    private String defaultBucket = bucket;

            @Override
            public S3ObjectId convert(Serializable id) {
                return new S3ObjectId(defaultBucket, Objects.requireNonNull(id, "ContentId must not be null").toString());
            }

		});

		// Object -> S3ObjectId
        conversion.addConverter(new Converter<Object, S3ObjectId>() {

            private String defaultBucket = bucket;

            @Override
            public S3ObjectId convert(Object idOrEntity) {

                String strBucket = null;
                Object bucket = BeanUtils.getFieldWithAnnotation(idOrEntity, Bucket.class);
                if (bucket == null) {
                    bucket = defaultBucket;
                }
                if (bucket == null) {
                    throw new StoreAccessException("Bucket not set");
                } else {
                    strBucket = bucket.toString();
                }


                // if an entity return @ContentId
                String key = null;
                if (BeanUtils.hasFieldWithAnnotation(idOrEntity, ContentId.class)) {
                    Object contentId = BeanUtils.getFieldWithAnnotation(idOrEntity, ContentId.class);
                    key = (contentId != null) ? contentId.toString() : null;
                // otherwise it is the id
                } else {
                    key = idOrEntity.toString();
                }

                return (key != null) ? new S3ObjectId(strBucket, key) : null;
            }

        });

        // ContentPropertyInfo -> S3ObjectId
        conversion.addConverter(new Converter<ContentPropertyInfo<Object, Serializable>, S3ObjectId>() {

            private String defaultBucket = bucket;

            @Override
            public S3ObjectId convert(ContentPropertyInfo<Object, Serializable> info) {
                // Object entity = info.getEntity();
                Serializable contentId = info.getContentId();
                // ContentProperty contentProperty = info.getContentProperty();

                String strBucket = null;
                // @Bucket can be only on entity level, not per content property
                Object bucket = BeanUtils.getFieldWithAnnotation(info.getEntity(), Bucket.class);
                if (bucket == null) {
                    bucket = defaultBucket;
                }
                if (bucket == null) {
                    throw new StoreAccessException("Bucket not set");
                } else {
                    strBucket = bucket.toString();
                }

                String key = null;
                if (contentId != null) {
                    key = contentId.toString();
                }

                return (key != null) ? new S3ObjectId(strBucket, key) : null;
            }

        });
	}

	private void addConverters(ConverterRegistry registry) {
		if (configurers == null)
			return;

		for (S3StoreConfigurer configurer : configurers) {
			configurer.configureS3StoreConverters(registry);
		}
	}
}
