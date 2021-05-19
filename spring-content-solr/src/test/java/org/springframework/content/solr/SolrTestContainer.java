package org.springframework.content.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

public class SolrTestContainer extends SolrContainer {

    private static final String CONNECTION_URL = "http://%s:%d/solr/solr";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("paulcwarren/solr").asCompatibleSubstituteFor("solr");

    private SolrTestContainer() {
        super(IMAGE_NAME);
        start();

        try {
            execInContainer("sh", "-c", "solr create_collection -c solr -d /opt/solr/server/solr/configsets/_default/");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to setup solr container", e);
        }
    }

    public static SolrClient getSolrClient() {
        String solrUrl = String.format(
                CONNECTION_URL,
                Singleton.INSTANCE.getContainerIpAddress(),
                Singleton.INSTANCE.getMappedPort(SolrContainer.SOLR_PORT));

        return new HttpSolrClient.Builder(solrUrl).build();
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected SolrTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final SolrTestContainer INSTANCE = new SolrTestContainer();
    }
}
