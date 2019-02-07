package org.springframework.content.s3;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * S3ObjectIdResolvers have been deprecated in favor of Converters.
 * <br>
 * <br>
 * This interface helps resolve the elements of an S3ObjectId object, namely bucket and
 * key. <br>
 * <br>
 * S3 stores can be configured with instances of this interface. See
 * {@link org.springframework.content.s3.config.S3StoreConfigurer}. Useful for stores who
 * wish to defer until runtime the decision about which bucket store resources in. <br>
 * <br>
 * The interface provides a static helper method
 * {@link #createS3ObjectIdResolver(Function, Function, Consumer)} which can be used to
 * provide an instance based on functional arguments.
 * <p>
 * Example:
 * <p>
 * {@code S3ObjectIdResolver.createS3ContentIdHeper(S3ObjectId::getBucket, S3ObjectId::getKey, id -> Assert.notNull(id.getKey, "ObjectId must not be null"))}
 *
 * @see org.springframework.core.convert.converter.Converter
 */
@Deprecated
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

	default Class<? extends I> getTarget() {
		return null;
	}

	static S3ObjectIdResolver<Serializable> createDefaultS3ObjectIdHelper() {
		return new S3ObjectIdResolver<Serializable>() {
		};
	}

	/**
	 * Creates a new S3ObjectIdResolver.
	 *
	 * @param getBucketFunction the function that provides the bucket name, or default bucket
	 * @param getObjectIdFunction the function that provides the object's key
	 * @param validateConsumer the function that validates the source to ensure a bucket and key can resolved
	 * @param <J> the source type for the resolver
	 * @return the resolver
	 */
	@Deprecated
	static <J> S3ObjectIdResolver<J> createS3ObjectIdResolver(
			Function<J, String> getBucketFunction,
			Function<J, String> getObjectIdFunction,
			Consumer<J> validateConsumer) {
		return new S3ObjectIdResolver<J>() {
			@Override
			public String getBucket(J idOrEntity, String defaultBucketName) {
				String bucketName = getBucketFunction.apply(idOrEntity);
				return null != bucketName ? bucketName : defaultBucketName;
			}

			@Override
			public String getKey(J idOrEntity) {
				return getObjectIdFunction.apply(idOrEntity);
			}

			@Override
			public void validate(J idOrEntity) {
				if (validateConsumer != null) {
					validateConsumer.accept(idOrEntity);
				}
			}
		};
	}

	/**
	 * Creates a new S3ObjectIdResolver.
	 *
	 * This variant of the factory method is used by {@link S3StoreConfiguration#s3StorePlacementService()}
	 * to convert <code>S3ObjectIdResolver</code>s into <code>Converter</code>s.  The <code>target</code>
	 * argument aids identification of the source type for the <code>Converter</code>.
	 *
	 * @param getBucketFunction the function that provides the bucket name, or default bucket
	 * @param getObjectIdFunction the function that provides the object's key
	 * @param validateConsumer the function that validates the source to ensure a bucket and key can resolved
	 * @param target the type of the source object that this resolver resolves from
	 * @param <I> the source type for the resolver
	 * @return the resolver
	 */
	@Deprecated
	static <I> S3ObjectIdResolver<I> createS3ObjectIdResolver(
			Function<I, String> getBucketFunction,
			Function<I, String> getObjectIdFunction,
			Consumer<I> validateConsumer,
			Class<? extends I> target) {
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

			@Override
			public Class<? extends I> getTarget() {
				return target;
			}
		};
	}
}
