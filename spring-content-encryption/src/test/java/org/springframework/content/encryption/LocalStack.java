package org.springframework.content.encryption;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class LocalStack extends LocalStackContainer implements Serializable {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("localstack/localstack");

    private LocalStack() {
        super(IMAGE_NAME);
        withServices(Service.S3);
        start();
    }

    private static class Singleton {
        private static final LocalStack INSTANCE = new LocalStack();
    }

    public static S3Client getAmazonS3Client() throws URISyntaxException {
        return S3Client.builder()
                .endpointOverride(new URI(Singleton.INSTANCE.getEndpointConfiguration(LocalStackContainer.Service.S3).getServiceEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(new CrossAwsCredentialsProvider(Singleton.INSTANCE.getDefaultCredentialsProvider()))
                .serviceConfiguration((bldr) -> bldr.pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public URI getEndpointOverride(EnabledService service) {
        try {
            // super method converts localhost to 127.0.0.1 which fails on macos
            // need to revert it back to whatever getContainerIpAddress() returns
            return new URIBuilder(super.getEndpointOverride(service)).setHost(getContainerIpAddress()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected LocalStack readResolve() {
        return Singleton.INSTANCE;
    }


    private static class CrossAwsCredentialsProvider implements AwsCredentialsProvider {
      private final AWSCredentials credentials;

      public CrossAwsCredentialsProvider(AWSCredentialsProvider provider) {
        this.credentials = provider.getCredentials();
      }

      @Override
      public AwsCredentials resolveCredentials() {
        return AwsBasicCredentials.create(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey());
      }
    }
}
