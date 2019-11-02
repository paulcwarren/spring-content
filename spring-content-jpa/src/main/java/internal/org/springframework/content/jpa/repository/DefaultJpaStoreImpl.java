package internal.org.springframework.content.jpa.repository;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.UUID;

import static java.lang.String.format;

public class DefaultJpaStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultJpaStoreImpl.class);

	private ResourceLoader loader;

	public DefaultJpaStoreImpl(ResourceLoader blobResourceLoader) {
		this.loader = blobResourceLoader;
	}

	@Override
	public Resource getResource(SID id) {
		return loader.getResource(id.toString());
	}

	@Override
	public Resource getResource(S entity) {
		Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (contentId == null) {
			return null;
		}

		return loader.getResource(contentId.toString());
	}

	@Override
	public void associate(S entity, SID id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
	}

	@Override
	public void unassociate(S entity) {
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
	public InputStream getContent(S entity) {
		Object id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (id == null) {
			return null;
		}
		Resource resource = loader.getResource(id.toString());
		try {
			return resource.getInputStream();
		}
		catch (IOException e) {
			logger.error(format("Unexpected error getting content for entity %s", entity), e);
			throw new StoreAccessException(format("Getting content for entity %s", entity), e);
		}
	}

	@Override
	public S setContent(S entity, InputStream content) {
		Resource resource = getResource(entity);
		if (resource == null) {
			UUID contentId = UUID.randomUUID();
			Object convertedId = convertToExternalContentIdType(entity, contentId);
			resource = this.getResource((SID)convertedId);
			BeanUtils.setFieldWithAnnotation(entity, ContentId.class,
					convertedId);
		}
		OutputStream os = null;
		long contentLen = -1L;
		try {
			if (resource instanceof WritableResource) {
				os = ((WritableResource) resource).getOutputStream();
				contentLen = IOUtils.copyLarge(content, os);
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}
		finally {
			IOUtils.closeQuietly(content);
			IOUtils.closeQuietly(os);
		}

		waitForCommit((BlobResource) resource);

		BeanUtils.setFieldWithAnnotation(entity, ContentId.class,
				((BlobResource) resource).getId());
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, contentLen);

		return entity;
	}

	private void waitForCommit(BlobResource resource) {
		synchronized (resource) {
			return;
		}
	}

	@Override
	public void unsetContent(S metadata) {
		Object id = BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
		if (id == null) {
			id = -1L;
		}
		Resource resource = loader.getResource(id.toString());
		if (resource instanceof DeletableResource) {
			try {
				((DeletableResource) resource).delete();
			} catch (Exception e) {
				logger.error(format("Unexpected error unsetting content for entity %s", metadata));
				throw new StoreAccessException(format("Unsetting content for entity %s", metadata), e);
			}
		}
		unassociate(metadata);
		BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, 0);
	}

	protected Object convertToExternalContentIdType(S property, Object contentId) {
		ConversionService converter = new DefaultConversionService();
		if (converter.canConvert(TypeDescriptor.forObject(contentId),
				TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
						ContentId.class)))) {
			contentId = converter.convert(contentId, TypeDescriptor.forObject(contentId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
							ContentId.class)));
			return contentId;
		}
		return contentId.toString();
	}

}
