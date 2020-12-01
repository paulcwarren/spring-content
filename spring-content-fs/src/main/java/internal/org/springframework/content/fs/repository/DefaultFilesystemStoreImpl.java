package internal.org.springframework.content.fs.repository;

import static java.lang.String.format;

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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.IdentifiableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional(readOnly = true)
public class DefaultFilesystemStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultFilesystemStoreImpl.class);

	private FileSystemResourceLoader loader;
	private PlacementService placer;
	private FileService fileService;

	public DefaultFilesystemStoreImpl(FileSystemResourceLoader loader, PlacementService conversion, FileService fileService) {
		this.loader = loader;
		this.placer = conversion;
		this.fileService = fileService;
	}

	@Override
	public Resource getResource(SID id) {
		String location = placer.convert(id, String.class);
		Resource resource = loader.getResource(location);
		((IdentifiableResource)resource).setId(id);
		return resource;
	}

	@Override
	public Resource getResource(S entity) {
		Resource resource = null;
		if (placer.canConvert(entity.getClass(), String.class)) {
			String location = placer.convert(entity, String.class);
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
    public Resource getResource(S entity, PropertyPath propertyPath) {

        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null) {
            UUID newId = UUID.randomUUID();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }
        return getResource(contentId);
    }

	@Override
	public void associate(S entity, SID id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
	}

    @Override
    public void associate(S entity, Resource resource, PropertyPath propertyPath) {

        SID contentId = null;
        if (resource instanceof IdentifiableResource) {
            contentId = (SID) ((IdentifiableResource)resource).getId();
        }
        setContentId(entity, propertyPath, contentId, null);
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
    public void unassociate(S entity, PropertyPath propertyPath) {

        setContentId(entity, propertyPath, null, new Condition() {
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
	public S setContent(S entity, InputStream content) {
        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {
            UUID newId = UUID.randomUUID();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

        Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
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
		} catch (IOException e) {
			logger.error(format("Unexpected io error setting content for entity %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		} catch (Exception e) {
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

		return entity;
	}

	@Transactional
	@Override
	public S setContent(S property, Resource resourceContent) {
		try {
			return this.setContent(property, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}
	}

	@Override
	@Transactional
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
	public S unsetContent(S entity) {
		if (entity == null)
			return entity;

		Resource resource = getResource(entity);

		if (resource != null && resource.exists() && resource instanceof DeletableResource) {
			try {
				((DeletableResource) resource).delete();
			} catch (IOException e) {
				logger.warn(format("Unable to get file for resource %s", resource));
			}
		}

		// reset content fields
		unassociate(entity);
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, 0);

		return entity;
	}

	private Object convertToExternalContentIdType(S property, Object contentId) {
		if (placer.canConvert(TypeDescriptor.forObject(contentId),
				TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
						ContentId.class)))) {
			contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
							ContentId.class)));
			return contentId;
		}
		return contentId.toString();
	}

    private SID getContentId(S entity, PropertyPath propertyPath) {

        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Field[] contentIdFields = BeanUtils.findFieldsWithAnnotation(entity.getClass(), ContentId.class, wrapper);
        for (Field contentIdField : contentIdFields) {
            if (contentIdField.getName().startsWith(propertyPath.getName())) {
                return (SID) wrapper.getPropertyValue(contentIdField.getName());
            }
        }
        throw new IllegalArgumentException(format("Invalid property path '%s'", propertyPath.getName()));
    }

    private void setContentId(S entity, PropertyPath propertyPath, SID contentId, Condition condition) {

        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        Field[] contentIdFields = BeanUtils.findFieldsWithAnnotation(entity.getClass(), ContentId.class, wrapper);
        for (Field contentIdField : contentIdFields) {
            if (contentIdField.getName().startsWith(propertyPath.getName())) {
                if (condition == null || condition.matches(contentIdField)) {
                    wrapper.setPropertyValue(contentIdField.getName(), contentId);
                }
                return;
            }
        }
        throw new IllegalArgumentException(format("Invalid property path '%s'", propertyPath.getName()));
    }
}