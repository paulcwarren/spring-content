package internal.org.springframework.content.elasticsearch;

import java.io.IOException;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.elasticsearch.AttributeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Autowired
    private RestHighLevelClient client;

    @Autowired(required=false)
    private RenditionService renditionService;

    @Autowired(required = false)
    private AttributeProvider attributeProvider;

    @Bean
    public IndexService elasticFulltextIndexService() throws IOException {
        return new ElasticsearchIndexServiceImpl(client, renditionService, indexManager(), attributeProvider);
    }

    @Bean
    public IndexManager indexManager() {
        return new IndexManager(client);
    }
}
