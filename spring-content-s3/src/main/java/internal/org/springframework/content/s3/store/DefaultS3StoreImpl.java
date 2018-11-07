package internal.org.springframework.content.s3.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.S3ObjectId;
import internal.org.springframework.content.s3.io.S3StoreResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

import static java.lang.String.format;

public class DefaultS3StoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

	private ResourceLoader loader;
	private ConversionService converter;
	private AmazonS3 client;
	private S3ObjectIdResolver idResolver = null;
	private String defaultBucket;

	public DefaultS3StoreImpl(ResourceLoader loader, ConversionService converter,
			AmazonS3 client, S3ObjectIdResolver idResolver, String defaultBucket) {
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(converter, "converter must be specified");
		Assert.notNull(client, "client must be specified");
		Assert.notNull(idResolver, "idResolver must be specified");
		this.loader = loader;
		this.converter = converter;
		this.client = client;
		this.idResolver = idResolver;
		this.defaultBucket = defaultBucket;
	}

	public S3ObjectIdResolver getS3ObjectIdResolver() {
		return idResolver;
	}

	@Override
	public Resource getResource(SID id) {
		if (id == null)
			return null;

		if (id instanceof S3ObjectId == false) {
			this.getS3ObjectIdResolver().validate(id);
			String bucket = this.getS3ObjectIdResolver().getBucket(id,
					this.defaultBucket);
			String objectId = this.getS3ObjectIdResolver().getKey(id);

			if (bucket == null) {
				throw new StoreAccessException("Bucket not set");
			}

			S3ObjectId s3ObjectId = new S3ObjectId(bucket, objectId);
			return this.getResourceInternal(s3ObjectId);
		}
		else {
			return this.getResourceInternal((S3ObjectId) id);
		}
	}

	@Override
	public Resource getResource(S entity) {
		if (entity == null)
			return null;

		String bucket = this.getS3ObjectIdResolver().getBucket(entity,
				this.defaultBucket);
		if (bucket == null) {
			throw new StoreAccessException("Bucket not set");
		}

		String key = this.getS3ObjectIdResolver().getKey(entity);
		if (key == null) {
			return null;
		}

		S3ObjectId s3ObjectId = new S3ObjectId(bucket.toString(), key.toString());
		return this.getResourceInternal(s3ObjectId);
	}

	protected Resource getResourceInternal(S3ObjectId id) {
		String bucket = id.getBucket();
		Object objectId = id.getKey();

		String location = converter.convert(objectId, String.class);
		location = absolutify(bucket, location);
		Resource resource = loader.getResource(location);
		return new S3StoreResource(client, bucket, resource);
	}

	@Override
	public void associate(Object entity, Serializable id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id);
	}

	@Override
	public void unassociate(Object entity) {
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("javax.persistence.Id".equals(
									annotation.annotationType().getCanonicalName())
									|| "org.springframework.data.annotation.Id"
											.equals(annotation.annotationType()
													.getCanonicalName())) {
								return false;
							}
						}
						return true;
					}
				});
	}

	@Override
	public void setContent(S entity, InputStream content) {
		Resource resource = this.getResource(entity);
		if (resource == null) {
			UUID newId = UUID.randomUUID();
			Object convertedId = converter.convert(newId, TypeDescriptor.forObject(newId),
					TypeDescriptor.valueOf(BeanUtils
							.getFieldWithAnnotationType(entity, ContentId.class)));
			resource = this.getResource((SID)convertedId);
			BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
		}

		OutputStream os = null;
		try {
			if (resource instanceof WritableResource) {
				os = ((WritableResource) resource).getOutputStream();
				IOUtils.copy(content, os);
				IOUtils.closeQuietly(os);
			}

			try {
				BeanUtils.setFieldWithAnnotation(entity, ContentLength.class,
						resource.contentLength());
			}
			catch (IOException e) {
				logger.error(format(
						"Unexpected error setting content length for entity %s",
						entity), e);
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", resource.toString()), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}
		finally {
			try {
				if (os != null) {
					os.close();
				}
			}
			catch (IOException ioe) {
				// ignore
			}
		}
	}

	@Override
	public InputStream getContent(S entity) {
		if (entity == null)
			return null;

		Resource resource = this.getResource(entity);

		try {
			if (resource != null && resource.exists()) {
				return resource.getInputStream();
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error getting content for entity %s", entity), e);
			throw new StoreAccessException(format("Getting content for entity %s", entity), e);
		}

		return null;
	}

	@Override
	public void unsetContent(S entity) {
		if (entity == null)
			return;

		deleteIfExists(entity);

		// reset content fields
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("javax.persistence.Id".equals(
									annotation.annotationType().getCanonicalName())
									|| "org.springframework.data.annotation.Id"
											.equals(annotation.annotationType()
													.getCanonicalName())) {
								return false;
							}
						}
						return true;
					}
				});
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, 0);
	}

	private String absolutify(String bucket, String location) {
		String locationToUse = null;
		Assert.state(location.startsWith("s3://") == false);
		if (location.startsWith("/")) {
			locationToUse = location.substring(1);
		}
		else {
			locationToUse = location;
		}
		return format("s3://%s/%s", bucket, locationToUse);
	}

	private void deleteIfExists(S entity) {
		String bucketName = this.getS3ObjectIdResolver().getBucket(entity,
				this.defaultBucket);

		Resource resource = this.getResource(entity);
		if (resource != null && resource.exists()) {
			try {
				client.deleteObject(new DeleteObjectRequest(bucketName, resource.getFilename()));
			} catch (AmazonClientException ace) {
				logger.error(format("Unexpected error unsetting content for entity %s", entity));
				throw new StoreAccessException(format("Unsetting content for entity %s", entity), ace);
			}
		}
	}
}
