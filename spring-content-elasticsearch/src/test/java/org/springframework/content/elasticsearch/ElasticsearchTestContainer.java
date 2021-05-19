package org.springframework.content.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class ElasticsearchTestContainer extends ElasticsearchContainer {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("paulcwarren/elasticsearch").asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

    // 9200:9200 -p 9300:9300 -e "discovery.type=single-node"

    private ElasticsearchTestContainer() {
        super(IMAGE_NAME);
        start();

//        try {
//            ExecResult r = execInContainer("sh", "-c", "bin/elasticsearch-plugin install --batch ingest-attachment");
//            int i=0;
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException("Failed to setup solr container", e);
//        }
    }

    public static RestHighLevelClient client() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(Singleton.INSTANCE.getContainerIpAddress(), Singleton.INSTANCE.getMappedPort(9200), "http")));
    }

    public static Object getUrl() {
        return String.format("http://%s:%d", Singleton.INSTANCE.getContainerIpAddress(), Singleton.INSTANCE.getMappedPort(9200));
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected ElasticsearchTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final ElasticsearchTestContainer INSTANCE = new ElasticsearchTestContainer();
    }
}
