package org.springframework.content.elasticsearch;

import internal.org.springframework.content.elasticsearch.IndexManager;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalIndexingStrategy implements IndexingStrategy {

    private static final String INDEX_NAME = IndexManager.INDEX_NAME;

    @Autowired
    private RestHighLevelClient client;

    public String indexName() {
        return INDEX_NAME;
    }

    public void setup() throws Exception {
        CreateIndexRequest cir = new CreateIndexRequest(INDEX_NAME);
        client.indices().create(cir, RequestOptions.DEFAULT);
    }
}
