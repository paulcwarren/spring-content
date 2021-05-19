package org.springframework.content.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

public class SolrTestContainer extends SolrContainer {

    private static final String CONNECTION_URL = "http://%s:%d/solr";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("solr").withTag("8.3.0");

    private SolrTestContainer() {
        super(IMAGE_NAME);
        start();
        // can use exec in container command here to set things up, e.g.
//        try {
//            execInContainer("solr -c create test");
//            execInContainer("solr command 2");
//            execInContainer("solr command 3");
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException("Failed to setup solr container", e);
//        }
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
