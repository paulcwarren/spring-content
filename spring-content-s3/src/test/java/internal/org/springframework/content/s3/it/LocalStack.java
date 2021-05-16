package internal.org.springframework.content.s3.it;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

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

    public static AmazonS3 getAmazonS3Client() {
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(Singleton.INSTANCE.getEndpointConfiguration(Service.S3))
                .withCredentials(Singleton.INSTANCE.getDefaultCredentialsProvider())
                .withPathStyleAccessEnabled(true)
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
}
