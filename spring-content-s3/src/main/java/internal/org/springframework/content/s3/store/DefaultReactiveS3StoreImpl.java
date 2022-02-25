package internal.org.springframework.content.s3.store;

import java.io.File;
import java.io.Serializable;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Transactional
public class DefaultReactiveS3StoreImpl<S, SID extends Serializable>
		implements ReactiveContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultReactiveS3StoreImpl.class);

	private ApplicationContext context;
	private ResourceLoader loader;
	private PlacementService placementService;
	private S3Client client;
	private MultiTenantS3ClientProvider clientProvider;

    private MappingContext mappingContext = new MappingContext(".", ".");

    // reactive
    private S3AsyncClient asyncClient;
    // reactive

	public DefaultReactiveS3StoreImpl(ApplicationContext context, ResourceLoader loader, PlacementService placementService, S3Client client, S3AsyncClient asyncClient, MultiTenantS3ClientProvider provider) {
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
    public Flux<ByteBuffer> getContentAsFlux(S entity, PropertyPath path) {

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
}
