package org.springframework.content.solr;


import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@ComponentScan(basePackages = {"org.springframework.content.solr"})
public class FullTextSolrIndexingConfig {

    @Autowired
    private SolrClient solrClient;
    @Autowired
    private SolrProperties props;
    @Autowired
    private ContentOperations ops;
    @Autowired
    private ConversionService contentConversionService;

    @Bean
    public ContentRepositoryExtension solrFulltextSearcher() {
        return new SolrSearchContentRepositoryExtension(solrClient, new ReflectionServiceImpl(), contentConversionService, props);
    }

    @Bean
    public Object solrFulltextEventListener() {
        return new SolrIndexer(solrClient, ops, props);
    }


}


