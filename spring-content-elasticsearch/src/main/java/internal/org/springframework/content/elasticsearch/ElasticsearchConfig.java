package internal.org.springframework.content.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.elasticsearch.AttributeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;

@Configuration
public class ElasticsearchConfig {

    @Autowired
    private RestHighLevelClient client;

    @Autowired(required=false)
    private RenditionService renditionService;

    @Autowired(required = false)
    private AttributeProvider attributeProvider;

    private List<RenditionProvider> providers = new ArrayList<>();

    @Autowired(required=false)
    public void setRenditionProviders(RenditionProvider... providers) {
        for (RenditionProvider provider : providers) {
            this.providers.add(provider);
        }
    }

    public RenditionService getRenditionService() {
        if (this.renditionService == null) {
            this.renditionService = new RenditionServiceImpl(providers.toArray(new RenditionProvider[0]));
        }
        return this.renditionService;
    }

    @Bean
    public IndexService elasticFulltextIndexService() throws IOException {
        return new ElasticsearchIndexServiceImpl(client, this.getRenditionService(), indexManager(), attributeProvider);
    }

    @Bean
    public IndexManager indexManager() {
        return new IndexManager(client);
    }
}
