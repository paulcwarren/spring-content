/*
 * 
 */
package internal.org.springframework.content.gcs.store;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.gcs.GCSObjectIdResolver;
import org.springframework.content.gcs.debug.DebugUtil;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;

import internal.org.springframework.content.gcs.io.GCSStoreResource;

/**
 * The Class DefaultGCSStoreImpl.
 *
 * @param <S> the generic type
 * @param <SID> the generic type
 */
public class DefaultGCSStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	/** The logger. */
	private static Log logger = LogFactory.getLog(DefaultGCSStoreImpl.class);

	/** The converter. */
	private ConversionService converter;
	
	/** The client. */
	private Storage client;
	
	/** The id resolver. */
	private GCSObjectIdResolver idResolver = null;
	
	/** The default bucket. */
	private String defaultBucket;

	/**
	 * Instantiates a new default GCS store impl.
	 *
	 * @param converter the converter
	 * @param client the client
	 * @param idResolver the id resolver
	 * @param defaultBucket the default bucket
	 */
	public DefaultGCSStoreImpl(ConversionService converter, Storage client, GCSObjectIdResolver idResolver,
			String defaultBucket) {
		DebugUtil.printCurrentMethod();
		Assert.notNull(converter, "converter must be specified");
		Assert.notNull(client, "client must be specified");
		Assert.notNull(idResolver, "idResolver must be specified");
		Assert.hasText(defaultBucket, "bucket must be specified");
		this.converter = converter;
		this.client = client;
		this.idResolver = idResolver;
		this.defaultBucket = defaultBucket;
	}

	/**
	 * Gets the GCS object id resolver.
	 *
	 * @return the GCS object id resolver
	 */
	public GCSObjectIdResolver getGCSObjectIdResolver() {
		DebugUtil.printCurrentMethod();
		return idResolver;
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.Store#getResource(java.io.Serializable)
	 */
	@Override
	public Resource getResource(SID id) {
		DebugUtil.printCurrentMethod();
		if (id == null) {
			return null;
		}
		if (id instanceof BlobId == false) {
			this.getGCSObjectIdResolver().validate(id);
			String bucket = this.getGCSObjectIdResolver().getBucket(id, this.defaultBucket);
			String objectId = this.getGCSObjectIdResolver().getKey(id);

			if (bucket == null) {
				throw new StoreAccessException("Bucket not set");
			}

			BlobId blobId = BlobId.of(bucket, objectId);
			return this.getResourceInternal(blobId);
		} else {
			return this.getResourceInternal((BlobId) id);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.AssociativeStore#getResource(java.lang.Object)
	 */
	@Override
	public Resource getResource(S entity) {
		DebugUtil.printCurrentMethod();
		if (entity == null)
			return null;

		String bucket = this.getGCSObjectIdResolver().getBucket(entity, this.defaultBucket);
		if (bucket == null) {
			throw new StoreAccessException("Bucket not set");
		}

		String key = this.getGCSObjectIdResolver().getKey(entity);
		if (key == null) {
			return null;
		}

		BlobId blobId = BlobId.of(bucket, key);
		return this.getResourceInternal(blobId);
	}

	/**
	 * Gets the resource internal.
	 *
	 * @param id the id
	 * @return the resource internal
	 */
	protected Resource getResourceInternal(BlobId id) {
		String bucket = id.getBucket();
		Object objectId = id.getName();

		String location = converter.convert(objectId, String.class);
		location = absolutify(bucket, location);

		System.out.println();
		
		Resource resource = new GoogleStorageResource(client, location);

		return new GCSStoreResource(client, bucket, resource);
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.AssociativeStore#associate(java.lang.Object, java.io.Serializable)
	 */
	@Override
	public void associate(Object entity, Serializable id) {
		DebugUtil.printCurrentMethod();
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id);
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.AssociativeStore#unassociate(java.lang.Object)
	 */
	@Override
	public void unassociate(Object entity) {
		DebugUtil.printCurrentMethod();
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, new Condition() {
			@Override
			public boolean matches(Field field) {
				for (Annotation annotation : field.getAnnotations()) {
					if ("javax.persistence.Id".equals(annotation.annotationType().getCanonicalName())
							|| "org.springframework.data.annotation.Id"
									.equals(annotation.annotationType().getCanonicalName())) {
						return false;
					}
				}
				return true;
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.ContentStore#setContent(java.lang.Object, java.io.InputStream)
	 */
	@Override
	public void setContent(S entity, InputStream content) {
		DebugUtil.printCurrentMethod();
		Resource resource = this.getResource(entity);
		if (resource == null) {
			UUID newId = UUID.randomUUID();
			Object convertedId = converter.convert(newId, TypeDescriptor.forObject(newId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(entity, ContentId.class)));
			resource = this.getResource((SID) convertedId);
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
				BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, resource.contentLength());
			} catch (IOException e) {
				logger.error(format("Unexpected error setting content length for entity %s", entity), e);
			}
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", resource.toString()), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException ioe) {
				// ignore
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.ContentStore#getContent(java.lang.Object)
	 */
	@Override
	public InputStream getContent(S entity) {
		DebugUtil.printCurrentMethod();
		if (entity == null)
			return null;

		Resource resource = this.getResource(entity);

		try {
			if (resource.exists()) {
				return resource.getInputStream();
			}
		} catch (IOException e) {
			logger.error(format("Unexpected error getting content for entity %s", entity), e);
			throw new StoreAccessException(format("Getting content for entity %s", entity), e);
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.content.commons.repository.ContentStore#unsetContent(java.lang.Object)
	 */
	@Override
	public void unsetContent(S entity) {
		DebugUtil.printCurrentMethod();
		if (entity == null) {
			return;
		}
		deleteIfExists(entity);

		// reset content fields
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null, new Condition() {
			@Override
			public boolean matches(Field field) {
				for (Annotation annotation : field.getAnnotations()) {
					if ("javax.persistence.Id".equals(annotation.annotationType().getCanonicalName())
							|| "org.springframework.data.annotation.Id"
									.equals(annotation.annotationType().getCanonicalName())) {
						return false;
					}
				}
				return true;
			}
		});
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, 0);
	}

	/**
	 * Absolutify.
	 *
	 * @param bucket the bucket
	 * @param location the location
	 * @return the string
	 */
	private String absolutify(String bucket, String location) {
		DebugUtil.printCurrentMethod();
		System.out.println("location" + location);
		String locationToUse = null;
		Assert.state(location.startsWith("gs://") == false);
		if (location.startsWith("/")) {
			locationToUse = location.substring(1);
		} else {
			locationToUse = location;
		}
		return format("gs://%s/%s", bucket, locationToUse);
	}

	/**
	 * Delete if exists.
	 *
	 * @param entity the entity
	 */
	private void deleteIfExists(S entity) {
		DebugUtil.printCurrentMethod();
		String bucketName = this.getGCSObjectIdResolver().getBucket(entity, this.defaultBucket);
		Resource resource = this.getResource(entity);

		if (resource != null && resource.exists()) {
			try {
				client.delete(bucketName, resource.getFilename());
			} catch (Exception ace) {
				logger.error(format("Unexpected error unsetting content for entity %s", entity));
				throw new StoreAccessException(format("Unsetting content for entity %s", entity), ace);
			}
		}
	}
}
