package internal.org.springframework.content.s3.store;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.ReactiveContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Transactional
public class DefaultS3StoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID>, ReactiveContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultS3StoreImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private S3Client client;
	private MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext = new MappingContext(".", ".");

    // reactive
    private S3AsyncClient asyncClient;
    // reactive

	public DefaultS3StoreImpl(ApplicationContext context, ResourceLoader loader, PlacementService placementService, S3Client client, S3AsyncClient asyncClient, MultiTenantS3ClientProvider provider) {
        Assert.notNull(context, "context must be specified");
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(placementService, "placementService must be specified");
		Assert.notNull(client, "client must be specified");
		this.context = context;
		this.loader = loader;
		this.placementService = placementService;
		this.client = client;
		this.asyncClient = asyncClient;
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

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return null;

        S3ObjectId s3ObjectId = null;
        if (placementService.canConvert(entity.getClass(), S3ObjectId.class)) {
            s3ObjectId = placementService.convert(entity, S3ObjectId.class);

            if (s3ObjectId != null) {
                return this.getResourceInternal(s3ObjectId);
            }
        }

        SID contentId = (SID) property.getContentId(entity);
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

        S3Client clientToUse = client;
        ResourceLoader loaderToUse = loader;
        if (clientProvider != null) {
			S3Client client = clientProvider.getS3Client();
			if (client != null) {
				SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
				s3Protocol.afterPropertiesSet();

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
    public void associate(S entity, PropertyPath propertyPath, SID id) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        property.setContentId(entity, id, null);
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

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {

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
            property.setContentLength(entity, resource.contentLength());
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
    public S setContent(S entity, PropertyPath propertyPath, Resource resourceContent) {
        try {
            return setContent(entity, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", entity), e);
            throw new StoreAccessException(format("Setting content for entity %s", entity), e);
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

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        if (entity == null)
            return entity;

        deleteIfExists(entity);

        // reset content fields
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
        property.setContentLength(entity, 0);

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
    public Mono<S> setContent(S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), path.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", path.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = placementService.convert(
                        newId,
                        TypeDescriptor.forObject(newId),
                        property.getContentIdType(entity));

            property.setContentId(entity, convertedId, null);
            contentId = convertedId;
        }

        if (placementService.canConvert(contentId.getClass(), S3ObjectId.class) == false) {
            throw new IllegalStateException(String.format("Unable to convert contentId %s to an S3ObjectId", contentId.toString()));
        }
        final S3ObjectId s3ObjectId = placementService.convert(contentId, S3ObjectId.class);

        CompletableFuture future = asyncClient.putObject(PutObjectRequest.builder()
            .bucket(s3ObjectId.getBucket())
            .contentLength(contentLen)
            .key(contentId.toString())
            .build(),

        AsyncRequestBody.fromPublisher(buffer));

        return Mono.fromFuture(future)
          .map((response) -> {
            property.setContentId(entity, s3ObjectId.getKey(), null);
            property.setContentLength(entity, contentLen);
            return entity;
          }
        );
    }

    @Override
    public Mono<Flux<ByteBuffer>> getContentAsFlux(S entity, PropertyPath path) {

        if (entity == null)
            return Mono.from(Flux.empty());

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), path.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", path.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {
            return Mono.from(Flux.empty());
        }

        if (placementService.canConvert(contentId.getClass(), S3ObjectId.class) == false) {
            throw new IllegalStateException(String.format("Unable to convert contentId %s to an S3ObjectId", contentId.toString()));
        }
        final S3ObjectId s3ObjectId = placementService.convert(contentId, S3ObjectId.class);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .key(contentId.toString())
                .build();

        return Mono.fromFuture(asyncClient.getObject(request,new FluxResponseProvider()))
                .map(response -> {
                  return response.flux;
                });
    }

    static class FluxResponseProvider implements AsyncResponseTransformer<GetObjectResponse,FluxResponse> {

        private FluxResponse response;

        @Override
        public CompletableFuture<FluxResponse> prepare() {
            response = new FluxResponse();
            return response.cf;
        }

        @Override
        public void onResponse(GetObjectResponse sdkResponse) {
            this.response.sdkResponse = sdkResponse;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            response.flux = Flux.from(publisher);
            response.cf.complete(response);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            response.cf.completeExceptionally(error);
        }
    }

    static class FluxResponse {
        final CompletableFuture<FluxResponse> cf = new CompletableFuture<>();
        GetObjectResponse sdkResponse;
        Flux<ByteBuffer> flux;
    }
}
