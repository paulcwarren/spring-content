package internal.org.springframework.content.elasticsearch;

import java.io.IOException;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.rest.RestStatus;

public class IndexManager {

    public static final String INDEX_NAME = "spring-content-fulltext-index";

    private final RestHighLevelClient client;

    private static Boolean globalIndexing = null;

    public IndexManager(RestHighLevelClient client) {
        this.client = client;
    }

    public String indexName(Class<?> entityClass) {

        if (globalIndexing == null) {
            try {
                client.indices().get(new GetIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
                globalIndexing = true;
            }
            catch (ElasticsearchStatusException ese) {
                if (ese.status() == RestStatus.NOT_FOUND) {
                    globalIndexing = false;
                } else {
                    // TODO: re-throw as StoreIndexException
                }
            }
            catch (IOException ioe) {
                // TODO: re-throw as StoreIndexException
            }
        }

        if (globalIndexing) {
            return INDEX_NAME;
        } else {
            return entityClass.getName().toLowerCase();
        }
    }

    public static void reset() {
        globalIndexing = null;
    }
}
