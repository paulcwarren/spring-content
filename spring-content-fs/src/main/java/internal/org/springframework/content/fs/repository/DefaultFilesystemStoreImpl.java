package internal.org.springframework.content.fs.repository;

import java.io.File;
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
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Version;

import static java.lang.String.format;

@Transactional(readOnly = true)
public class DefaultFilesystemStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultFilesystemStoreImpl.class);

	private FileSystemResourceLoader loader;
	private ConversionService conversion;
	private FileService fileService;

	public DefaultFilesystemStoreImpl(FileSystemResourceLoader loader, ConversionService conversion, FileService fileService) {
		this.loader = loader;
		this.conversion = conversion;
		this.fileService = fileService;
	}

	@Override
	public Resource getResource(SID id) {
		String location = conversion.convert(id, String.class);
		Resource resource = loader.getResource(location);
		return resource;
	}

	@Override
	public Resource getResource(S entity) {
		Resource resource = null;
		if (conversion.canConvert(entity.getClass(), String.class)) {
			String location = conversion.convert(entity, String.class);
			resource = loader.getResource(location);
			if (resource != null) {
				return resource;
			}
		}

		SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (contentId != null) {
			return getResource(contentId);
		}

		return null;
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
	@Transactional
	public void setContent(S entity, InputStream content) {
		Resource resource = getResource(entity);
		if (resource == null) {
			UUID contentId = UUID.randomUUID();
			Object convertedId = convertToExternalContentIdType(entity, contentId);
			resource = getResource((SID)convertedId);
			BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
		}
		OutputStream os = null;
		try {
			if (resource.exists() == false) {
				File resourceFile = resource.getFile();
				File parent = resourceFile.getParentFile();
				this.fileService.mkdirs(parent);
			}
			if (resource instanceof WritableResource) {
				os = ((WritableResource) resource).getOutputStream();
				IOUtils.copy(content, os);
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}
		finally {
			IOUtils.closeQuietly(os);
		}

		try {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class,
					resource.contentLength());
		}
		catch (IOException e) {
			logger.error(format(
					"Unexpected error setting content length for content for resource %s",
					resource.toString()), e);
		}
	}

	@Override
	public InputStream getContent(S entity) {
		if (entity == null)
			return null;

		Resource resource = getResource(entity);

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
	@Transactional
	public void unsetContent(S entity) {
		if (entity == null)
			return;

		Resource resource = getResource(entity);

		if (resource != null && resource.exists() && resource instanceof DeletableResource) {
			File parent = null;
			try {
				parent = resource.getFile().getParentFile();
				((DeletableResource) resource).delete();
			} catch (IOException e) {
				logger.warn(format("Unable to get file for resource %s", resource));
			}

			File root = loader.getRootResource().getFile();
			if (parent != null) {
				try {
					fileService.rmdirs(parent, root);
				} catch (IOException e) {
					logger.warn(format("Removing orphaned directories from %s to %s for resource %s", parent.getAbsolutePath(), root.getAbsolutePath(), resource));
				}
			}
		}

		// reset content fields
		unassociate(entity);
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, 0);
	}

	private Object convertToExternalContentIdType(S property, Object contentId) {
		if (conversion.canConvert(TypeDescriptor.forObject(contentId),
				TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
						ContentId.class)))) {
			contentId = conversion.convert(contentId, TypeDescriptor.forObject(contentId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
							ContentId.class)));
			return contentId;
		}
		return contentId.toString();
	}
}