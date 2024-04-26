package internal.org.springframework.content.s3.store;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Transactional
public class DefaultReactiveS3StoreImpl<S, SID extends Serializable>
		implements org.springframework.content.commons.repository.ReactiveContentStore<S, SID>,
        ReactiveContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultReactiveS3StoreImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext/* = new MappingContext("/", ".")*/;

    private S3AsyncClient asyncClient;

	public DefaultReactiveS3StoreImpl(ApplicationContext context, ResourceLoader loader, MappingContext mappingContext, PlacementService placementService, S3AsyncClient asyncClient, MultiTenantS3ClientProvider provider) {
        Assert.notNull(context, "context must be specified");
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(placementService, "placementService must be specified");
		this.context = context;
		this.loader = loader;
		this.placementService = placementService;
		this.asyncClient = asyncClient;
		this.clientProvider = provider;
        this.mappingContext = mappingContext;
        if (this.mappingContext == null) {
            this.mappingContext = new MappingContext("/", ".");
        }
	}

    private S3ObjectId getS3ObjectId(S entity, PropertyPath path, ContentProperty property) {
        TypeDescriptor contentPropertyInfoType = ContentPropertyInfoTypeDescriptor.withGenerics(entity, property);
        if (!placementService.canConvert(contentPropertyInfoType, TypeDescriptor.valueOf(S3ObjectId.class))) {
            throw new IllegalStateException(String.format("Unable to convert %s to an S3ObjectId", contentPropertyInfoType));
        }
        ContentPropertyInfo<S, SID> contentPropertyInfo = ContentPropertyInfo.of(entity, (SID) property.getContentId(entity), path, property);
        S3ObjectId s3ObjectId = (S3ObjectId) placementService.convert(contentPropertyInfo, contentPropertyInfoType, TypeDescriptor.valueOf(S3ObjectId.class));
        return s3ObjectId;
    }

    @Transactional
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

        final S3ObjectId s3ObjectId = getS3ObjectId(entity, path, property);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .contentLength(contentLen)
                .key(s3ObjectId.getKey());

        Object mimeType = property.getMimeType(entity);
        if (mimeType != null) {
            String strMimeType = mimeType.toString();
            requestBuilder.contentType(strMimeType);
        }

        CompletableFuture<PutObjectResponse> future = asyncClient.putObject(
                requestBuilder.build(),
                AsyncRequestBody.fromPublisher(buffer)
        );

        return Mono.fromFuture(future)
          .map((response) -> {
              try {
                  property.setContentId(entity, s3ObjectId.getKey(), null);
              } catch (Exception e) {
                  logger.error("Error setting content id " + s3ObjectId.getKey(), e);
                  throw e;
              }
              try {
                  property.setContentLength(entity, contentLen);
              } catch (Exception e) {
                  logger.error("Error setting content length " + contentLen, e);
                  throw e;
              }
              return entity;
          }
        );
    }

    @Override
    public Flux<ByteBuffer> getContent(S entity, PropertyPath path) {

        if (entity == null)
            return Flux.empty();

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), path.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", path.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {
            return Flux.empty();
        }

        final S3ObjectId s3ObjectId = getS3ObjectId(entity, path, property);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .key(s3ObjectId.getKey())
                .build();

        AsyncResponseTransformer.toFile(new File("/tmp/foo"));
        CompletableFuture<ResponsePublisher<GetObjectResponse>> responseFuture =
                asyncClient.getObject(request, AsyncResponseTransformer.toPublisher());

        ResponsePublisher<GetObjectResponse> responsePublisher = responseFuture.join();
        return Flux.from(responsePublisher);
    }

    @Transactional
    @Override
    public Mono<S> unsetContent(S entity, PropertyPath propertyPath) {

        if (entity == null)
            return Mono.just(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }

        Object contentId = property.getContentId(entity);
        if (contentId == null) {
            return Mono.just(entity);
        }

        final S3ObjectId s3ObjectId = getS3ObjectId(entity, propertyPath, property);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .key(s3ObjectId.getKey())
                .build();

        CompletableFuture<DeleteObjectResponse> future = asyncClient.deleteObject(deleteRequest);

        return Mono.fromFuture(future)
                .map((response) -> {
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
                    property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
                    return entity;
                }
              );
    }
}
