package internal.org.springframework.content.autoconfigure.solr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.solr.SolrProperties;
import org.springframework.content.solr.SolrSearchImpl;
import org.springframework.content.solr.SolrUpdateEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@ConditionalOnBean({ SolrClient.class })
//@EnableConfigurationProperties(org.springframework.content.solr.SolrProperties.class)
public class SolrAutoConfiguration {

    @Autowired private SolrProperties props;
    @Autowired private SolrClient solrClient;
	@Autowired private ContentOperations ops;
    @Autowired private ConversionService contentConversionService;

	public SolrAutoConfiguration() {
	}
	
	@Bean
	public Object solrFulltextEventListener() {
		return new SolrUpdateEventHandler(solrClient, ops);
	}

    @Bean
    public Searchable<Object> solrFulltextSearcher() {
        return new SolrSearchImpl(solrClient, new ReflectionServiceImpl(), contentConversionService, props);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix="spring-content-solr")
    public SolrProperties solrProperties() {
        return new SolrProperties();
    }
}
