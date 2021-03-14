package internal.org.springframework.content.azure.config;

import static java.lang.String.format;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.content.azure.Bucket;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;

import internal.org.springframework.content.azure.store.BlobId;

public class BlobIdResolverConverter implements GenericConverter {

	private final String defaultBucket;

	private static Set<ConvertiblePair> convertibleTypes = new HashSet<>();

	static {
	    convertibleTypes.add(new ConvertiblePair(Serializable.class, BlobId.class));
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
		return (key != null) ? new BlobId(bucket, key) : null;
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

        return source.toString();
    }
}
