package internal.org.springframework.content.gcs.config;

import static java.lang.String.format;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.gcs.Bucket;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;

import com.google.cloud.storage.BlobId;

public class BlobIdResolverConverter implements GenericConverter {

	private final String defaultBucket;
	private Class<?> fromType;

	private static Set<ConvertiblePair> convertibleTypes = new HashSet<>();

	static {
	    convertibleTypes.add(new ConvertiblePair(Serializable.class, BlobId.class));
        convertibleTypes.add(new ConvertiblePair(Object.class, BlobId.class));
	}

	public BlobIdResolverConverter(String defaultBucket) {
		this.defaultBucket = defaultBucket;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return convertibleTypes;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

	    String bucket = this.getBucket(source, defaultBucket);
		Assert.notNull(bucket, format("Unable to determine bucket from %s", source));
		String key = this.getKey(source);
		return (key != null) ? BlobId.of(bucket, key) : null;
	}

    private String getBucket(Object source, String defaultBucketName) {

        Object bucket = BeanUtils.getFieldWithAnnotation(source, Bucket.class);
        if (bucket == null) {
            bucket = defaultBucketName;
        }
        if (bucket == null) {
            throw new StoreAccessException("Bucket not set");
        }
        return bucket.toString();
    }

    private String getKey(Object source) {

        if (BeanUtils.hasFieldWithAnnotation(source, ContentId.class)) {
            Object contentId = BeanUtils.getFieldWithAnnotation(source, ContentId.class);
            return contentId != null ? contentId.toString() : null;
        } else {
            return source.toString();
        }
    }
}
