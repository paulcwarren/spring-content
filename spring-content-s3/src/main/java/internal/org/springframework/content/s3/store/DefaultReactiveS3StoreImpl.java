package internal.org.springframework.content.s3.store;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ReactiveContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
		implements ReactiveContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultReactiveS3StoreImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext = new MappingContext(".", ".");

    private S3AsyncClient asyncClient;

	public DefaultReactiveS3StoreImpl(ApplicationContext context, ResourceLoader loader, PlacementService placementService, S3AsyncClient asyncClient, MultiTenantS3ClientProvider provider) {
        Assert.notNull(context, "context must be specified");
		Assert.notNull(loader, "loader must be specified");
		Assert.notNull(placementService, "placementService must be specified");
		this.context = context;
		this.loader = loader;
		this.placementService = placementService;
		this.asyncClient = asyncClient;
		this.clientProvider = provider;
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

        if (placementService.canConvert(contentId.getClass(), S3ObjectId.class) == false) {
            throw new IllegalStateException(String.format("Unable to convert contentId %s to an S3ObjectId", contentId.toString()));
        }
        final S3ObjectId s3ObjectId = placementService.convert(contentId, S3ObjectId.class);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .contentLength(contentLen)
                .key(contentId.toString());

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
            property.setContentId(entity, s3ObjectId.getKey(), null);
            property.setContentLength(entity, contentLen);
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

        if (placementService.canConvert(contentId.getClass(), S3ObjectId.class) == false) {
            throw new IllegalStateException(String.format("Unable to convert contentId %s to an S3ObjectId", contentId.toString()));
        }
        final S3ObjectId s3ObjectId = placementService.convert(contentId, S3ObjectId.class);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .key(contentId.toString())
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

        if (placementService.canConvert(contentId.getClass(), S3ObjectId.class) == false) {
            throw new IllegalStateException(String.format("Unable to convert contentId %s to an S3ObjectId", contentId.toString()));
        }
        final S3ObjectId s3ObjectId = placementService.convert(contentId, S3ObjectId.class);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3ObjectId.getBucket())
                .key(contentId.toString())
                .build();

        CompletableFuture<DeleteObjectResponse> future = asyncClient.deleteObject(deleteRequest);

        return Mono.fromFuture(future)
                .map((response) -> {
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
              );
    }
}
