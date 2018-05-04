package org.springframework.content.s3;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * This interface helps resolve the elements of an S3ObjectId object, namely bucket and key.
 * <br/><br/>
 * S3 stores can be configured with instances of this interface.  See {@link org.springframework.content.s3.config.S3StoreConfigurer}.
 * Useful for stores who wish to defer until runtime the decision about which bucket store resources in.
 * <br/><br/>
 * The interface provides a static helper method {@link #createS3ObjectIdResolver(Function, Function, Consumer)} which
 * can be used to provide an instance based on functional arguments.
 * <p>
 * Example:
 * <p>
 * {@code S3ObjectIdResolver.createS3ContentIdHeper(S3ObjectId::getBucket, S3ObjectId::getKey,
		id -> Assert.notNull(id.getKey, "ObjectId must not be null"))}
 */
public interface S3ObjectIdResolver<I> {

	default String getBucket(I idOrEntity, String defaultBucketName) {
		return defaultBucketName;
	}

	default String getKey(I idOrEntity) {
		return Objects.requireNonNull(idOrEntity, "ContentId must not be null").toString();
	}

	default void validate(I idOrEntity) {
		Assert.notNull(idOrEntity, "ContentId must not be null");
	}
	
	static <I> S3ObjectIdResolver<I> createDefaultS3ObjectIdHelper() {
		return new S3ObjectIdResolver<I>() {};
	}
	
	static <I> S3ObjectIdResolver<I> createS3ObjectIdResolver(
			Function<I, String> getBucketFunction, Function<I, String> getObjectIdFunction, Consumer<I> validateConsumer) {
		return new S3ObjectIdResolver<I>() {
			@Override
			public String getBucket(I idOrEntity, String defaultBucketName) {
				String bucketName = getBucketFunction.apply(idOrEntity);
				return null != bucketName ? bucketName : defaultBucketName;
			}
			
			@Override
			public String getKey(I idOrEntity) {
				return getObjectIdFunction.apply(idOrEntity);
			}
			
			@Override
			public void validate(I idOrEntity) {
				if (validateConsumer != null) {
					validateConsumer.accept(idOrEntity);
				}
			}
		};
	}
}
