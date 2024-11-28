package internal.org.springframework.content.s3.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.springframework.content.commons.io.RangeableResource;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

import org.springframework.util.FastByteArrayOutputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * {@link org.springframework.core.io.Resource} implementation for
 * {@code com.amazonaws.services.s3.model.S3Object} handles. Implements the extended
 * {@link WritableResource} interface.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageResource extends AbstractResource implements WritableResource, RangeableResource {

    private final String bucketName;

    private final String objectName;

    private final String versionId;

    private final S3Client amazonS3;

    private final TaskExecutor taskExecutor;

    private volatile HeadObjectResponse objectMetadata;

    private String range;

    private String contentType;

    public SimpleStorageResource(S3Client amazonS3, String bucketName, String objectName,
            TaskExecutor taskExecutor) {
        this(amazonS3, bucketName, objectName, taskExecutor, null, null);
    }

    public SimpleStorageResource(S3Client amazonS3, String bucketName, String objectName,
                                 TaskExecutor taskExecutor, String versionId) {
        this(amazonS3, bucketName, objectName, taskExecutor, versionId, null);
    }

    public SimpleStorageResource(S3Client amazonS3, String bucketName, String objectName,
            TaskExecutor taskExecutor, String versionId, String contentType) {
//        this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.taskExecutor = taskExecutor;
        this.versionId = versionId;
        this.contentType = contentType;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder("Amazon s3 resource [bucket='");
        builder.append(this.bucketName);
        builder.append("' and object='");
        builder.append(this.objectName);
        if (this.versionId != null) {
            builder.append("' and versionId='");
            builder.append(this.versionId);
        }
        builder.append("']");
        return builder.toString();
    }

    /**
     * Set the Content-Type value that will be specified as object metadata when saving resource to the object storage.
     * @param contentType Content-Type value or null
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Determine the Content-Type value of the resource as saved in object storage.
     * @return Content-Type value of the resource
     * @throws IOException
     */
    public String contentType() throws IOException {
        return getRequiredObjectMetadata().contentType();
    }

    @Override
    public void setRange(String range) {
        this.range = range;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                .bucket(this.bucketName).key(this.objectName);
        if (this.versionId != null) {
            getObjectRequestBuilder.versionId(this.versionId);
        }
        if (this.range != null) {
            getObjectRequestBuilder.range(range);
            var getObjectResponse = this.amazonS3.getObject(getObjectRequestBuilder.build());
            var sdkResponse = getObjectResponse.response().sdkHttpResponse();
            switch (sdkResponse.statusCode()) {
                case 206: // Partial content
                    var contentRange = getObjectResponse.response().contentRange();
                    // As per https://www.rfc-editor.org/rfc/rfc9110.html#name-206-partial-content,
                    // a Content-Range header MUST be present when responding with a single part.
                    if(contentRange == null) {
                        throw new IOException("Received HTTP 206, but missing a Content-Range header. Only single-part responses are supported.");
                    }
                    return PartialContentInputStream.fromContentRange(
                            getObjectResponse,
                            contentRange
                    );
                case 200: // OK -> this is a full response
                    return getObjectResponse;
                default:
                    throw new IOException("Unexpected HTTP response code %d %s".formatted(sdkResponse.statusCode(), sdkResponse.statusText().orElse("")));
            }
        }
        return this.amazonS3.getObject(getObjectRequestBuilder.build());
    }

    @Override
    public boolean exists() {
        return getObjectMetadata() != null;
    }

    @Override
    public long contentLength() throws IOException {
        return getRequiredObjectMetadata().contentLength();
    }

    @Override
    public long lastModified() throws IOException {
        return getRequiredObjectMetadata().lastModified().getEpochSecond();
    }

    @Override
    public String getFilename() throws IllegalStateException {
        return this.objectName;
    }

    @Override
    public URL getURL() {
        return this.amazonS3.utilities().getUrl(GetUrlRequest.builder()
                .bucket(this.bucketName).key(this.objectName).build());
    }

    @Override
    public File getFile() throws IOException {
        throw new UnsupportedOperationException(
                "Amazon S3 resource can not be resolved to java.io.File objects.Use "
                        + "getInputStream() to retrieve the contents of the object!");
    }

    private HeadObjectResponse getRequiredObjectMetadata() throws FileNotFoundException {
        HeadObjectResponse metadata = getObjectMetadata();
        if (metadata == null) {
            StringBuilder builder = new StringBuilder().append("Resource with bucket='")
                    .append(this.bucketName).append("' and objectName='")
                    .append(this.objectName);
            if (this.versionId != null) {
                builder.append("' and versionId='");
                builder.append(this.versionId);
            }
            builder.append("' not found!");

            throw new FileNotFoundException(builder.toString());
        }
        return metadata;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new SimpleStorageOutputStream();
    }

    @Override
    public SimpleStorageResource createRelative(String relativePath) throws IOException {
        String relativeKey = this.objectName + "/" + relativePath;
        return new SimpleStorageResource(this.amazonS3, this.bucketName, relativeKey,
                this.taskExecutor);
    }

    private HeadObjectResponse getObjectMetadata() {
        if (this.objectMetadata == null) {
            try {
                HeadObjectRequest.Builder headObjectRequestBuilder = HeadObjectRequest
                        .builder().bucket(this.bucketName).key(this.objectName);
                if (this.versionId != null) {
                    headObjectRequestBuilder.versionId(this.versionId);
                }
                this.objectMetadata = this.amazonS3
                        .headObject(headObjectRequestBuilder.build());
            }
            catch (S3Exception e) {
                // Catch 404 (object not found) and 301 (bucket not found, moved
                // permanently)
                if (e.statusCode() == 404 || e.statusCode() == 301) {
                    this.objectMetadata = null;
                }
                else {
                    throw e;
                }
            }
        }
        return this.objectMetadata;
    }

    private class SimpleStorageOutputStream extends OutputStream {

        // The minimum size for a multi part is 5 MB, hence the buffer size of 5 MB
        private static final int BUFFER_SIZE = 1024 * 1024 * 5;

        private final Object monitor = new Object();

        private final CompletionService<CompletedPart> completionService;

        @SuppressWarnings("FieldMayBeFinal")
        private FastByteArrayOutputStream currentOutputStream = new FastByteArrayOutputStream();

        private int partNumberCounter = 1;

        private CreateMultipartUploadResponse multiPartUploadResult;

        SimpleStorageOutputStream() {
            this.completionService = new ExecutorCompletionService<>(
                    new ExecutorServiceAdapter(SimpleStorageResource.this.taskExecutor));
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (this.monitor) {
                if (this.currentOutputStream.size() == BUFFER_SIZE) {
                    initiateMultiPartIfNeeded();
                    this.completionService.submit(new UploadPartResultCallable(
                            SimpleStorageResource.this.amazonS3,
                            this.currentOutputStream.toByteArray(),
                            this.currentOutputStream.size(),
                            SimpleStorageResource.this.bucketName,
                            SimpleStorageResource.this.objectName,
                            this.multiPartUploadResult.uploadId(),
                            this.partNumberCounter++));
                    this.currentOutputStream.reset();
                }
                this.currentOutputStream.write(b);
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (this.monitor) {
                if (this.currentOutputStream == null) {
                    return;
                }

                if (isMultiPartUpload()) {
                    finishMultiPartUpload();
                }
                else {
                    finishSimpleUpload();
                }
            }
        }

        private boolean isMultiPartUpload() {
            return this.multiPartUploadResult != null;
        }

        private void finishSimpleUpload() {
            byte[] content = this.currentOutputStream.toByteArray();
            String md5Digest;
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                md5Digest = BinaryUtils.toBase64(messageDigest.digest(content));
            }
            catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(
                        "MessageDigest could not be initialized because it uses an unknown algorithm",
                        e);
            }

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(SimpleStorageResource.this.bucketName)
                .key(SimpleStorageResource.this.objectName)
                .contentMD5(md5Digest);

            if (SimpleStorageResource.this.contentType != null) {
                requestBuilder.contentType(SimpleStorageResource.this.contentType);
            }

            SimpleStorageResource.this.amazonS3.putObject(requestBuilder.build(), RequestBody.fromBytes(content));

            // Release the memory early
            this.currentOutputStream = null;
        }

        private void finishMultiPartUpload() throws IOException {
            this.completionService.submit(new UploadPartResultCallable(
                    SimpleStorageResource.this.amazonS3,
                    this.currentOutputStream.toByteArray(),
                    this.currentOutputStream.size(),
                    SimpleStorageResource.this.bucketName,
                    SimpleStorageResource.this.objectName,
                    this.multiPartUploadResult.uploadId(), this.partNumberCounter));
            try {
                CompletedMultipartUpload multipartUpload = CompletedMultipartUpload
                        .builder().parts(getCompletedMultiParts()).build();
                SimpleStorageResource.this.amazonS3
                        .completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                                .bucket(this.multiPartUploadResult.bucket())
                                .key(this.multiPartUploadResult.key())
                                .multipartUpload(multipartUpload)
                                .uploadId(this.multiPartUploadResult.uploadId()).build());
            }
            catch (ExecutionException e) {
                abortMultiPartUpload();
                throw new IOException("Multi part upload failed ", e.getCause());
            }
            catch (InterruptedException e) {
                abortMultiPartUpload();
                Thread.currentThread().interrupt();
            }
            finally {
                this.currentOutputStream = null;
            }
        }

        private void initiateMultiPartIfNeeded() {
            if (this.multiPartUploadResult == null) {
                CreateMultipartUploadRequest.Builder requestBuilder = CreateMultipartUploadRequest.builder()
                        .bucket(SimpleStorageResource.this.bucketName)
                        .key(SimpleStorageResource.this.objectName);

                if (SimpleStorageResource.this.contentType != null) {
                    requestBuilder.contentType(SimpleStorageResource.this.contentType);
                }

                this.multiPartUploadResult = SimpleStorageResource.this.amazonS3
                        .createMultipartUpload(requestBuilder.build());
            }
        }

        private void abortMultiPartUpload() {
            if (isMultiPartUpload()) {
                SimpleStorageResource.this.amazonS3
                        .abortMultipartUpload(AbortMultipartUploadRequest.builder()
                                .bucket(this.multiPartUploadResult.bucket())
                                .key(this.multiPartUploadResult.key())
                                .uploadId(this.multiPartUploadResult.uploadId()).build());
            }
        }

        private List<CompletedPart> getCompletedMultiParts()
                throws ExecutionException, InterruptedException {
            List<CompletedPart> result = new ArrayList<>(this.partNumberCounter);
            for (int i = 0; i < this.partNumberCounter; i++) {
                Future<CompletedPart> uploadPartResultFuture = this.completionService
                        .take();
                result.add(uploadPartResultFuture.get());
            }
            return result;
        }

        private final class UploadPartResultCallable implements Callable<CompletedPart> {

            private final S3Client amazonS3;

            private final int contentLength;

            private final int partNumber;

            private final String bucketName;

            private final String key;

            private final String uploadId;

            @SuppressWarnings("FieldMayBeFinal")
            private byte[] content;

            private UploadPartResultCallable(S3Client amazon, byte[] content,
                    int writtenDataSize, String bucketName, String key, String uploadId,
                    int partNumber) {
                this.amazonS3 = amazon;
                this.content = content;
                this.contentLength = writtenDataSize;
                this.partNumber = partNumber;
                this.bucketName = bucketName;
                this.key = key;
                this.uploadId = uploadId;
            }

            @Override
            public CompletedPart call() throws Exception {
                try {
                    final UploadPartResponse uploadPartResponse = this.amazonS3
                            .uploadPart(
                                    UploadPartRequest.builder().bucket(this.bucketName)
                                            .key(this.key).uploadId(this.uploadId)
                                            .partNumber(this.partNumber).build(),
                                    RequestBody.fromInputStream(
                                            new ByteArrayInputStream(this.content),
                                            this.contentLength));
                    return CompletedPart.builder().partNumber(this.partNumber)
                            .eTag(uploadPartResponse.eTag()).build();

                }
                finally {
                    // Release the memory, as the callable may still live inside the
                    // CompletionService which would cause
                    // an exhaustive memory usage
                    this.content = null;
                }
            }

        }

    }

}
