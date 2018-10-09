package internal.org.springframework.content.gcs.config;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.gcs.Bucket;
import org.springframework.content.gcs.GCSObjectIdResolver;
import org.springframework.core.convert.ConversionService;

/**
 * The default associative store GCSObjectId resolver.
 *
 * This default implementation will look for fields annotated with @Bucket to
 * resolve the name of the bucket. It will look for fields annotated
 * with @ContentId to resolve the key.
 */
public class DefaultAssociativeStoreGCSObjectIdResolver implements GCSObjectIdResolver<Object> {

	private ConversionService converter;

	public DefaultAssociativeStoreGCSObjectIdResolver(ConversionService converter) {
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
		if (BeanUtils.hasFieldWithAnnotation(idOrEntity, ContentId.class)) {
			Object contentId = BeanUtils.getFieldWithAnnotation(idOrEntity, ContentId.class);
			return contentId != null ? contentId.toString() : null;
		} else {
			return idOrEntity.toString();
		}
	}

}
