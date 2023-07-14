package internal.org.springframework.content.jpa.repository;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.*;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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

    private MappingContext mappingContext/* = new MappingContext("/", ".")*/;

	public DefaultJpaStoreImpl(ResourceLoader blobResourceLoader, MappingContext mappingContext) {
		this.loader = blobResourceLoader;
		this.mappingContext = mappingContext;
		if (this.mappingContext == null) {
		    this.mappingContext = new MappingContext("/", ".");
		}
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
    public Resource getResource(S entity, PropertyPath propertyPath) {
        return this.getResource(entity, propertyPath, GetResourceParams.builder().build());
    }

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null) {
            return null;
        }
        return getResource(contentId);
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        property.setContentId(entity, id, null);
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        property.setContentId(entity, null, new org.springframework.content.commons.mappingcontext.Condition() {
            @Override
            public boolean matches(TypeDescriptor descriptor) {
                for (Annotation annotation : descriptor.getAnnotations()) {
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
    public InputStream getContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }
        Object id = property.getContentId(entity);
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

	@Transactional
	@Override
	public S setContent(S entity, InputStream content) {

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

		Resource resource = getResource(entity);
		if (resource == null) {
		    return entity;
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

		BeanUtils.setFieldWithAnnotation(entity, ContentId.class,
				((BlobResource) resource).getId());
		BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, contentLen);

		return entity;
	}

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content) {
        return this.setContent(entity, propertyPath, content, -1L);
    }

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        SID contentId = getContentId(entity, propertyPath);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = convertToExternalContentIdType(newId, property.getContentIdType(entity));

            setContentId(entity, propertyPath, (SID)convertedId, null);
        }

        Resource resource = getResource(entity, propertyPath);
        if (resource == null) {
            return entity;
        }

        OutputStream os = null;
        long readLen = -1L;
        try {
            if (resource instanceof WritableResource) {
                os = ((WritableResource) resource).getOutputStream();
                readLen = IOUtils.copyLarge(content, os);
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

        property.setContentId(entity, ((BlobResource) resource).getId(), null);

        long len = contentLen;
        if (len == -1L) {
            len = readLen;
        }
        property.setContentLength(entity, len);

        return entity;
    }

    @Transactional
	@Override
	public S setContent(S entity, Resource resourceContent) {
		try {
			return this.setContent(entity, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}
	}

    @Transactional
    @Override
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        try {
            return this.setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
        }
    }

	@Transactional
	@Override
	public S unsetContent(S metadata) {
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

		return metadata;
	}

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Resource resource = this.getResource(entity, propertyPath);

        if (resource != null && resource.exists() && resource instanceof DeletableResource) {
            try {
                ((DeletableResource) resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }

        if (resource != null) {
            unassociate(entity, propertyPath);
            property.setContentLength(entity, 0);
        }

        return entity;
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

    private Object convertToExternalContentIdType(Object contentId, TypeDescriptor contentIdType) {
        ConversionService converter = new DefaultConversionService();
        if (converter.canConvert(TypeDescriptor.forObject(contentId),
                contentIdType)) {
            contentId = converter.convert(contentId, TypeDescriptor.forObject(contentId),
                    contentIdType);
            return contentId;
        }
        return contentId.toString();
    }

    private SID getContentId(S entity, PropertyPath propertyPath) {

        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        return (SID) property.getContentId(entity);
    }

    private void setContentId(S entity, PropertyPath propertyPath, SID contentId, Condition condition) {

        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }
        property.setContentId(entity, contentId,  null);
    }
}
