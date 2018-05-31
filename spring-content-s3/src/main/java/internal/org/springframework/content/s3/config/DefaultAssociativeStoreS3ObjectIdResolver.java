package internal.org.springframework.content.s3.config;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.util.UUID;

/**
 * The default associative store S3ObjectId resolver.
 *
 * This default implementation will look for fields annotated with @Bucket to resolve the
 * name of the bucket. It will look for fields annotated with @ContentId to resolve the
 * key.
 */
public class DefaultAssociativeStoreS3ObjectIdResolver
		implements S3ObjectIdResolver<Object> {

	private ConversionService converter;

	public DefaultAssociativeStoreS3ObjectIdResolver(ConversionService converter) {
		this.converter = converter;
	}

	@Override
	public String getBucket(Object idOrEntity, String defaultBucketName) {
		Object bucket = BeanUtils.getFieldWithAnnotation(idOrEntity, Bucket.class);
		if (bucket == null) {
			bucket = defaultBucketName;
		}
		if (bucket == null) {
			throw new StoreAccessException("Bucket not set");
		}
		return bucket.toString();
	}

	@Override
	public String getKey(Object idOrEntity) {
		// if an entity return @ContentId
		if (BeanUtils.hasFieldWithAnnotation(idOrEntity, ContentId.class)) {
			Object contentId = BeanUtils.getFieldWithAnnotation(idOrEntity, ContentId.class);
			return contentId != null ? contentId.toString() : null;
		// otherwise is is the id
		} else {
			return idOrEntity.toString();
		}
	}

}
