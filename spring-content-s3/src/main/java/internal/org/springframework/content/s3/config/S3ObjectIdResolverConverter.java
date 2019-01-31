package internal.org.springframework.content.s3.config;

import com.amazonaws.services.s3.model.S3ObjectId;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;

/**
 * A Converter wrapper for the {@link org.springframework.content.s3.S3ObjectIdResolver}s.
 */
public class S3ObjectIdResolverConverter implements GenericConverter {

	private final S3ObjectIdResolver resolver;
	private final String defaultBucket;
	private Class<?> fromType;

	public S3ObjectIdResolverConverter(S3ObjectIdResolver resolver, String defaultBucket) {
		this.resolver = resolver;
		this.defaultBucket = defaultBucket;
		this.fromType = getFromType(resolver);
		Assert.notNull(this.fromType, format("Unable to determine type of S3ObjectIdResolver %s", resolver));
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(fromType, S3ObjectId.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		resolver.validate(source);
		String bucket = resolver.getBucket(source, defaultBucket);
		Assert.notNull(bucket, format("Unable to determine bucket from %s", source));
		String key = resolver.getKey(source);
		return (key != null) ? new S3ObjectId(bucket, key) : null;
	}

	protected Class<?> getFromType(S3ObjectIdResolver resolver) {
		if (resolver.getTarget() != null)
			return resolver.getTarget();

		Class<?> fromType = null;
		Type[] types = resolver.getClass().getGenericInterfaces();

		for (Type t : types) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				Type[] typeArgs = pt.getActualTypeArguments();
				if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
					fromType = (Class<?>)typeArgs[0];
				}
			}
		}
		return fromType;
	}
}
