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
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.SetContentParams;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;

import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import internal.org.springframework.content.gcs.io.GCSResource;

@Transactional
public class DefaultGCPStorageImpl<S, SID extends Serializable>
		implements org.springframework.content.commons.repository.Store<SID>,
		org.springframework.content.commons.repository.AssociativeStore<S, SID>,
		org.springframework.content.commons.repository.ContentStore<S, SID>,
		ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultGCPStorageImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private Storage client;
//	private MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext/* = new MappingContext("/", ".")*/;

	public DefaultGCPStorageImpl(ApplicationContext context, ResourceLoader loader, MappingContext mappingContext, PlacementService placementService, Storage client2) {
        Assert.notNull(context, "context must be specified");
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(placementService, "placementService must be specified");
		Assert.notNull(client2, "client must be specified");
		this.context = context;
		this.loader = loader;
		this.placementService = placementService;
		this.client = client2;
//		this.clientProvider = provider;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
	}

	@Override
	public Resource getResource(SID id) {
		if (id == null)
			return null;

		if (id instanceof BlobId == false) {
			BlobId blobId = null;
			if (placementService.canConvert(id.getClass(), BlobId.class)) {
				blobId = placementService.convert(id, BlobId.class);
				return this.getResourceInternal(blobId);
			}

			throw new StoreAccessException(format("Unable to convert from %s to BlobId", id));
		} else {
			return this.getResourceInternal((BlobId) id);
		}
	}

	@Override
	public Resource getResource(S entity) {
		if (entity == null)
			return null;

		BlobId blobId = null;
		if (placementService.canConvert(entity.getClass(), BlobId.class)) {
			blobId = placementService.convert(entity, BlobId.class);

			if (blobId != null) {
				return this.getResourceInternal(blobId);
			}
		}

		SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		return this.getResource(contentId);
	}

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
		return this.getResource(entity, propertyPath, GetResourceParams.builder().build());
    }

	@Override
	public Resource getResource(S entity, PropertyPath propertyPath, org.springframework.content.commons.repository.GetResourceParams params) {
		return this.getResource(entity, propertyPath, GetResourceParams.builder().range(params.getRange()).build());
	}

	@Override
	public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
		ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
		if (property == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		if (entity == null)
			return null;

		BlobId blobId = null;
		TypeDescriptor contentPropertyInfoType = ContentPropertyInfoTypeDescriptor.withGenerics(entity, property);
		if (placementService.canConvert(contentPropertyInfoType, TypeDescriptor.valueOf(BlobId.class))) {
			ContentPropertyInfo<S, SID> contentPropertyInfo = ContentPropertyInfo.of(entity,
					(SID) property.getContentId(entity), propertyPath, property);
			blobId = placementService.convert(contentPropertyInfo, BlobId.class);

			if (blobId != null) {
				return this.getResourceInternal(blobId);
			}
		}

		SID contentId = (SID) property.getContentId(entity);
		return this.getResource(contentId);
	}

	protected Resource getResourceInternal(BlobId id) {
		String bucket = id.getBucket();

        String location = null;
        if (placementService.canConvert(BlobId.class, String.class)) {
            location = placementService.convert(id, String.class);
            location = absolutify(bucket, location);
        } else {
            Object objectId = id.getName();
            location = placementService.convert(objectId, String.class);
            location = absolutify(bucket, location);
        }

        Storage clientToUse = client;
        ResourceLoader loaderToUse = loader;
//        if (clientProvider != null) {
//			Storage client = clientProvider.getAmazonS3();
//			if (client != null) {
//				SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver();
//				s3Protocol.afterPropertiesSet();
//				s3Protocol.setBeanFactory(context);
//
//				DefaultResourceLoader loader = new DefaultResourceLoader();
//				loader.addProtocolResolver(s3Protocol);
//
//				clientToUse = client;
//				loaderToUse = loader;
//			}
//		}

		Resource resource = loaderToUse.getResource(location);
		return new GCSResource(clientToUse, bucket, resource);
	}

	@Override
	public void associate(Object entity, Serializable id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id);
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
                    if ("jakarta.persistence.Id".equals(
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
	public void unassociate(Object entity) {
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("jakarta.persistence.Id".equals(
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

	@Transactional
	@Override
	public S setContent(S entity, InputStream content) {

        Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

			Object convertedId = placementService.convert(
						newId,
						TypeDescriptor.forObject(newId),
						TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(entity, ContentId.class)));

			BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
		}

		Resource resource = this.getResource(entity);
		if (resource == null) {
			return entity;
		}

		if (resource instanceof WritableResource) {
    		try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
    				IOUtils.copy(content, os);
    		}
    		catch (IOException e) {
    		    logger.error(format("Unexpected error setting content for entity %s", entity), e);
    		    throw new StoreAccessException(format("Setting content for entity %s", entity), e);
    		}
		}

		try {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, resource.contentLength());
		}
		catch (IOException e) {
			logger.error(format("Unexpected error setting content length for entity %s", entity), e);
		}
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
		return this.setContent(entity, propertyPath, content,
				org.springframework.content.commons.store.SetContentParams.builder()
						.contentLength(contentLen)
						.build());
	}

	@Override
	public S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
		return this.setContent(entity, propertyPath, content,
				org.springframework.content.commons.store.SetContentParams.builder()
						.contentLength(params.getContentLength())
						.overwriteExistingContent(params.isOverwriteExistingContent())
						.build());
	}

	@Override
	public S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params) {
		ContentProperty property = this.mappingContext .getContentProperty(entity.getClass(), propertyPath.getName());
		if (property == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		Object contentId = property.getContentId(entity);
		if (contentId == null || !params.isOverwriteExistingContent()) {

			Serializable newId = UUID.randomUUID().toString();

			Object convertedId = placementService.convert(
					newId,
					TypeDescriptor.forObject(newId),
					property.getContentIdType(entity));

			property.setContentId(entity, convertedId, null);
		}

		Resource resource = this.getResource(entity, propertyPath);
		if (resource == null) {
			return entity;
		}

		if (resource instanceof WritableResource) {
			try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
				IOUtils.copy(content, os);
			}
			catch (IOException e) {
				logger.error(format("Unexpected error setting content for entity %s", entity), e);
				throw new StoreAccessException(format("Setting content for entity %s", entity), e);
			}
		}

		try {
			long len = params.getContentLength();
			if (len == -1L) {
				len = resource.contentLength();
			}
			property.setContentLength(entity, len);
		}
		catch (IOException e) {
			logger.error(format("Unexpected error setting content length for entity %s", entity), e);
		}
		return entity;
	}

	@Override
	public S setContent(S property, Resource resourceContent) {
		try {
			return setContent(property, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}
	}

    @Override
    public S setContent(S property, PropertyPath propertyPath, Resource resourceContent) {
        try {
            return setContent(property, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

	@Transactional
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

    @Transactional
    @Override
    public InputStream getContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return null;

        Resource resource = this.getResource(entity, propertyPath);

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

	@Transactional
	@Override
	public S unsetContent(S entity) {
		if (entity == null)
			return entity;

		deleteIfExists(entity);

		// reset content fields
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("jakarta.persistence.Id".equals(
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

		return entity;
	}

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return entity;

        Resource resource = this.getResource(entity, propertyPath);
        if (resource != null && resource.exists() && resource instanceof DeletableResource) {

            try {
                ((DeletableResource)resource).delete();
            } catch (Exception e) {
                logger.error(format("Unexpected error unsetting content for entity %s", entity));
                throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
            }
        }

        // reset content fields
        property.setContentId(entity, null, new org.springframework.content.commons.mappingcontext.Condition() {
            @Override
            public boolean matches(TypeDescriptor descriptor) {
                for (Annotation annotation : descriptor.getAnnotations()) {
                    if ("jakarta.persistence.Id".equals(
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
        property.setContentLength(entity, 0);

        return entity;
    }

	private String absolutify(String bucket, String location) {
		String locationToUse = null;
		Assert.state(location.startsWith("gs://") == false);
		if (location.startsWith("/")) {
			locationToUse = location.substring(1);
		}
		else {
			locationToUse = location;
		}
		return format("gs://%s/%s", bucket, locationToUse);
	}

	private void deleteIfExists(S entity) {

		Resource resource = this.getResource(entity);
		if (resource != null && resource.exists() && resource instanceof DeletableResource) {

			try {
				((DeletableResource)resource).delete();
			} catch (Exception e) {
				logger.error(format("Unexpected error unsetting content for entity %s", entity));
				throw new StoreAccessException(format("Unsetting content for entity %s", entity), e);
			}
		}
	}
}
