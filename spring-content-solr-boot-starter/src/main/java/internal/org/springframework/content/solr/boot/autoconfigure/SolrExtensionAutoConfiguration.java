package internal.org.springframework.content.solr.boot.autoconfigure;

import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.solr.SolrProperties;
import org.springframework.content.solr.SolrSearchContentRepositoryExtension;
import org.springframework.content.solr.SolrIndexer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@ComponentScan(basePackageClasses = SolrAutoConfiguration.class)
public class SolrExtensionAutoConfiguration {

    @Autowired private SolrProperties props;
    @Autowired private SolrClient solrClient;
	@Autowired private ContentOperations ops;
    @Autowired private ConversionService contentConversionService;

	public SolrExtensionAutoConfiguration() {
	}
	
	@Bean
	public Object solrFulltextEventListener() {
		return new SolrIndexer(solrClient, ops, props);
	}

    @Bean
    public ContentRepositoryExtension solrFulltextSearcher() {
        return new SolrSearchContentRepositoryExtension(solrClient, new ReflectionServiceImpl(), contentConversionService, props);
    }

}
