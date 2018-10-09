package org.springframework.content.gcs;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * This interface helps resolve the elements of an GCSObjectId object, namely
 * bucket and key. <br>
 * <br>
 * GCS stores can be configured with instances of this interface. See
 * {@link org.springframework.content.GCSStoreConfigurer.config.GCSStoreConfigurer}.
 * Useful for stores who wish to defer until runtime the decision about which
 * bucket store resources in. <br>
 * <br>
 * The interface provides a static helper method
 * {@link #createGCSObjectIdResolver(Function, Function, Consumer)} which can be
 * used to provide an instance based on functional arguments.
 * <p>
 * Example:
 * <p>
 * {@code GCSObjectIdResolver.createGCSContentIdHeper(GCSObjectId::getBucket, GCSObjectId::getKey,
		id -> Assert.notNull(id.getKey, "ObjectId must not be null"))}
 */
public interface GCSObjectIdResolver<I> {

	default String getBucket(I idOrEntity, String defaultBucketName) {
		return defaultBucketName;
	}

	default String getKey(I idOrEntity) {
		return Objects.requireNonNull(idOrEntity, "ContentId must not be null").toString();
	}

	default void validate(I idOrEntity) {
		Assert.notNull(idOrEntity, "ContentId must not be null");
	}

	static <I> GCSObjectIdResolver<I> createDefaultGCSObjectIdHelper() {
		return new GCSObjectIdResolver<I>() {
		};
	}

	static <I> GCSObjectIdResolver<I> createGCSObjectIdResolver(Function<I, String> getBucketFunction,
			Function<I, String> getObjectIdFunction, Consumer<I> validateConsumer) {
		return new GCSObjectIdResolver<I>() {
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
