package internal.org.springframework.content.s3.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

public class DefaultS3StoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

	private ResourceLoader loader;
	private ConversionService converter;
	private AmazonS3 client;
	private String bucket;

	/**
	 * Constructs default implementation of content store.
	 * 
	 * @param loader
	 *            Resource loader.
	 * @param converter
	 *            Identification converter.
	 * @param client
	 *            S3 client driver.
	 * @param bucket
	 *            S3 bucket name.
	 */
	public DefaultS3StoreImpl(ResourceLoader loader, ConversionService converter, AmazonS3 client, String bucket) {
		this.loader = loader;
		this.converter = converter;
		this.client = client;
		this.bucket = bucket;
	}

	@Override
	public Resource getResource(SID id) {
		String location = converter.convert(id, String.class);
		location = absolutify(location);
		Resource resource = loader.getResource(location);
		return resource;
	}

	@Override
	public void associate(S entity, SID id) {
		String location = converter.convert(id, String.class);
		location = absolutify(location);
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, location);
		Resource resource = loader.getResource(location);
		try {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, resource.contentLength());
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content length for %s", location), e);
		}
	}

	@Override
	public void unassociate(S entity) {
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
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, 0L);
	}

	@Override
	public void setContent(S property, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			contentId = UUID.randomUUID().toString();
			BeanUtils.setFieldWithAnnotation(property, ContentId.class, contentId);
		}

		String location = converter.convert(contentId, String.class);
		location = absolutify(location);
		Resource resource = loader.getResource(location);
		OutputStream os = null;
		try {
			if (resource instanceof WritableResource) {
				os = ((WritableResource) resource).getOutputStream();
				IOUtils.copy(content, os);
			}
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content %s", contentId.toString()), e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException ioe) {
				// ignore
			}
		}

		try {
			BeanUtils.setFieldWithAnnotation(property, ContentLength.class, resource.contentLength());
		} catch (IOException e) {
			logger.error(String.format("Unexpected error setting content length for content %s", contentId.toString()),
					e);
		}
	}

	@Override
	public InputStream getContent(S property) {
		if (property == null) {
			return null;
		}
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			return null;
		}

		String location = converter.convert(contentId, String.class);
		location = absolutify(location);
		Resource resource = loader.getResource(location);
		try {
			if (resource.exists()) {
				return resource.getInputStream();
			}
		} catch (IOException e) {
			logger.error(String.format("Unexpected error getting content %s", contentId.toString()), e);
		}

		return null;
	}

	@Override
	public void unsetContent(S property) {
		if (property == null) {
			return;
		}
		Object contentId = BeanUtils.getFieldWithAnnotation(property, ContentId.class);
		if (contentId == null) {
			return;
		}

		// delete any existing content object
		try {
			String location = converter.convert(contentId, String.class);
			location = absolutify(location);
			Resource resource = loader.getResource(location);
			if (resource.exists()) {
				this.delete(resource);
			}

			// reset content fields
			BeanUtils.setFieldWithAnnotationConditionally(property, ContentId.class, null, new Condition() {
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
			BeanUtils.setFieldWithAnnotation(property, ContentLength.class, 0);
		} catch (Exception ase) {
			logger.error(String.format("Unexpected error unsetting content %s", contentId.toString()), ase);
		}
	}

	private String absolutify(String location) {
		String locationToUse = null;
		Assert.state(location.startsWith("s3://") == false);
		if (location.startsWith("/")) {
			locationToUse = location.substring(1);
		} else {
			locationToUse = location;
		}
		return String.format("s3://%s/%s", bucket, locationToUse);
	}

	private void delete(Resource resource) {
		if (resource.exists()) {
			client.deleteObject(new DeleteObjectRequest(bucket, resource.getFilename()));
		}
	}

}
