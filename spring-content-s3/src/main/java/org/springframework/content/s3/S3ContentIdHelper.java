package org.springframework.content.s3;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * This interface helps handling custom types for content id fields by implementing methods to get the relevant
 * fields <em>bucket</em> and <em>objctId</em> of the custom type. 
 * <p>
 * The interface provides a static method {@link #createS3ContentIdHelper(Function, Function, Consumer)} which could
 * be used to provide an instance passing functional arguments.
 * <p>
 * Example:
 * <p>
 * {@code S3ContentIdHelper.createS3ContentIdHeper(S3ContentId::getBucket, S3ContentId::getObjectId, 
		id -> Assert.notNull(id.getObjectId, "ObjectId must not be null"))}
 */
public interface S3ContentIdHelper<I> {
	default String getBucket(I contentId, String defaultBucketName) {
		return defaultBucketName;
	}
	default String getObjectId(I contentId) {
		return Objects.requireNonNull(contentId, "ContentId must not be null").toString();
	}
	default void validate(I contentId) {
		Assert.notNull(contentId, "ContentId must not be null");
	}
	
	static <I> S3ContentIdHelper<I> createDefaultS3ContentIdHelper() {
		return new S3ContentIdHelper<I>() {};
	}
	
	static <I> S3ContentIdHelper<I> createS3ContentIdHelper(
			Function<I, String> getBucketFunction, Function<I, String> getObjectIdFunction, Consumer<I> validateConsumer) {
		return new S3ContentIdHelper<I>() {
			@Override
			public String getBucket(I contentId, String defaultBucketName) {
				String bucketName = getBucketFunction.apply(contentId);
				return null != bucketName ? bucketName : defaultBucketName;
			}
			
			@Override
			public String getObjectId(I contentId) {
				return getObjectIdFunction.apply(contentId);
			}
			
			@Override
			public void validate(I contentId) {
				validateConsumer.accept(contentId);
			}
		};
	}
}
