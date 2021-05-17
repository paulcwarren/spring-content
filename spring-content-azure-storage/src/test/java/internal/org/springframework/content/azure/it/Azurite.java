package internal.org.springframework.content.azure.it;

import java.io.Serializable;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.containers.GenericContainer;

/**
 * This class provides a TestContainers implementation of Azure storage via
 * an Azurite docker container.
 *
 * Please refer to the following for details:-
 * <a href="https://www.testcontainers.org">http://www.testcontainers.org</a>
 * <a href="http://github.com/testcontainers/testcontainers-java">http://github.com/testcontainers/testcontainers-java</a>
 * <a href="http://github.com/Azure/Azurite">http://github.com/Azure/Azurite</a>
 */

public class Azurite extends GenericContainer<Azurite> implements Serializable {

    // Will default to latest tag on every test run
    private static final String DOCKER_IMAGE_NAME = "mcr.microsoft.com/azure-storage/azurite";

    // Default config as per Azurite docs
    private static final int BLOB_SERVICE_PORT = 10000;

    @SuppressWarnings("HttpUrlsUsage") // Testing only
    private static final String ENDPOINT = "http://%s:%d/%s";
    private static final String DEV_ACC_NAME = "devstoreaccount1";
    private static final String PROTOCOL = "DefaultEndpointsProtocol=http";
    private static final String ACC_NAME = "AccountName=" + DEV_ACC_NAME;
    private static final String ACC_KEY =
            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private Azurite() {
        super(DOCKER_IMAGE_NAME);
        this.start();
    }

    public static BlobServiceClientBuilder getBlobServiceClientBuilder() {
        final String host = Singleton.INSTANCE.getContainerIpAddress();
        final Integer mappedPort = Singleton.INSTANCE.getMappedPort(BLOB_SERVICE_PORT);
        final String endpoint = String.format(ENDPOINT, host, mappedPort, DEV_ACC_NAME);

        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .connectionString(String.join(";", PROTOCOL, ACC_NAME, ACC_KEY, "BlobEndpoint=" + endpoint));
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected Azurite readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final Azurite INSTANCE = new Azurite();
    }
}
