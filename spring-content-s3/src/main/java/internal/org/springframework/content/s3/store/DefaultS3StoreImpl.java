package internal.org.springframework.content.s3.store;

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
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.config.MultiTenantAmazonS3Provider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectId;

import internal.org.springframework.content.s3.io.S3StoreResource;

@Transactional
public class DefaultS3StoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private AmazonS3 client;
	private MultiTenantAmazonS3Provider clientProvider;

	public DefaultS3StoreImpl(ApplicationContext context, ResourceLoader loader, PlacementService placementService, AmazonS3 client, MultiTenantAmazonS3Provider provider) {
        Assert.notNull(context, "context must be specified");
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(placementService, "placementService must be specified");
		Assert.notNull(client, "client must be specified");
		this.context = context;
		this.loader = loader;
		this.placementService = placementService;
		this.client = client;
		this.clientProvider = provider;
	}

	@Override
	public Resource getResource(SID id) {
		if (id == null)
			return null;

		if (id instanceof S3ObjectId == false) {
			S3ObjectId s3ObjectId = null;
			if (placementService.canConvert(id.getClass(), S3ObjectId.class)) {
				s3ObjectId = placementService.convert(id, S3ObjectId.class);
				return this.getResourceInternal(s3ObjectId);
			}

			throw new StoreAccessException(format("Unable to convert from %s to S3ObjectId", id));
		} else {
			return this.getResourceInternal((S3ObjectId) id);
		}
	}

	@Override
	public Resource getResource(S entity) {
		if (entity == null)
			return null;

		S3ObjectId s3ObjectId = null;
		if (placementService.canConvert(entity.getClass(), S3ObjectId.class)) {
			s3ObjectId = placementService.convert(entity, S3ObjectId.class);

			if (s3ObjectId != null) {
				return this.getResourceInternal(s3ObjectId);
			}
		}

		SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		return this.getResource(contentId);
	}

	protected Resource getResourceInternal(S3ObjectId id) {
		String bucket = id.getBucket();

        String location = null;
        if (placementService.canConvert(S3ObjectId.class, String.class)) {
            location = placementService.convert(id, String.class);
            location = absolutify(bucket, location);
        } else {
            Object objectId = id.getKey();
            location = placementService.convert(objectId, String.class);
            location = absolutify(bucket, location);
        }

        AmazonS3 clientToUse = client;
        ResourceLoader loaderToUse = loader;
        if (clientProvider != null) {
			AmazonS3 client = clientProvider.getAmazonS3();
			if (client != null) {
				SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver();
				s3Protocol.afterPropertiesSet();
				s3Protocol.setBeanFactory(context);

				DefaultResourceLoader loader = new DefaultResourceLoader();
				loader.addProtocolResolver(s3Protocol);

				clientToUse = client;
				loaderToUse = loader;
			}
		}

		Resource resource = loaderToUse.getResource(location);
		return new S3StoreResource(clientToUse, bucket, resource);
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

	@Override
	public S setContent(S property, Resource resourceContent) {
		try {
			return setContent(property, resourceContent.getInputStream());
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

		return entity;
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

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {
        throw new UnsupportedOperationException();
    }
}
