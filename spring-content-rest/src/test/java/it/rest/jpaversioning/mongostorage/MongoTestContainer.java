package it.rest.jpaversioning.mongostorage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

public class MongoTestContainer extends MongoDBContainer {

    private static final String DOCKER_IMAGE_NAME = "mongo";

    private MongoTestContainer() {
        super(DOCKER_IMAGE_NAME);
        start();
    }

    public static String getTestDbName() {
        return StringUtils.substringAfterLast(Singleton.INSTANCE.getReplicaSetUrl(), "/");

    }

    public static String getTestDbUrl() {
        return StringUtils.substringBeforeLast(Singleton.INSTANCE.getReplicaSetUrl(), "/");
    }

    public static MongoClient getMongoClient() {
        return MongoClients.create(getTestDbUrl());
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected MongoTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final MongoTestContainer INSTANCE = new MongoTestContainer();
    }
}
