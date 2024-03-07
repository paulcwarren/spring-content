package internal.org.springframework.content.s3.it;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.services.s3.S3Client;

public class LocalStack extends LocalStackContainer implements Serializable {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("localstack/localstack");

    private static LocalStack INSTANCE = null;

    private LocalStack() {
        super(IMAGE_NAME);
        withServices(Service.S3);
        start();
    }

    public static LocalStack singleton() {
        if (INSTANCE == null) {
            INSTANCE = new LocalStack();
        }
        return INSTANCE;
    }

    public static S3Client getAmazonS3Client() throws URISyntaxException {
        return S3Client.builder()
                .endpointOverride(new URI(LocalStack.singleton().getEndpointConfiguration(LocalStackContainer.Service.S3).getServiceEndpoint()))
                .credentialsProvider(new LocalStack.CrossAwsCredentialsProvider(LocalStack.singleton().getDefaultCredentialsProvider()))
                .serviceConfiguration((serviceBldr) -> {serviceBldr.pathStyleAccessEnabled(true);})
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
        return singleton();
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
