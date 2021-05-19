package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;

public class GCSStorageContainer /*extends GenericContainer<GCSStorageContainer>*/ {

    private static final String DOCKER_IMAGE_NAME = "fsouza/fake-gcs-server:v1.24.0";

    private static Storage storage = null;

    private GCSStorageContainer() {
//        super(DOCKER_IMAGE_NAME);
//        this.setExposedPorts(Collections.singletonList(4443));
//        this.setCommand("foo");
//        this.start();

    }

    public static Storage getStorage() {

//        if (storage == null) {
//            GenericContainer gcs = new GenericContainer(
//                    new ImageFromDockerfile()
//                            .withDockerfileFromBuilder(builder ->
//                                    builder
//                                            .from(DOCKER_IMAGE_NAME)
//                                            .entryPoint("/bin/fake-gcs-server -scheme http")
//                                            .build()))
//                            .withExposedPorts(4443);
//
//            gcs.start();
//
//            final String host = gcs.getContainerIpAddress();
//            final Integer mappedPort = gcs.getMappedPort(4443);
//            final String endpoint = String.format("http://%s:%s", host, mappedPort);
//
//            storage = StorageOptions.newBuilder()
//                .setHost(endpoint)
//                .build().getService();
//
//            storage.create(BucketInfo.of("delete-me-please-please"));
//        }
//
//        return storage;

        if (storage == null) {
            storage = LocalStorageHelper.getOptions().getService();
//            storage.create(BucketInfo.of("delete-me-please-please"));
        }

        return storage;
    }
}
