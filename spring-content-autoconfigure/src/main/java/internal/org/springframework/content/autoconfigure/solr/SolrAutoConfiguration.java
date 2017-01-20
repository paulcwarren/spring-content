package internal.org.springframework.content.autoconfigure.solr;

import internal.org.springframework.content.commons.search.SolrSearchImpl;
import internal.org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.search.Searchable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean({ SolrClient.class })
public class SolrAutoConfiguration {

	@Autowired private SolrClient solrClient;
	@Autowired private ContentOperations ops;

	public SolrAutoConfiguration() {
	}
	
	@Bean
	public Object solrFulltextEventListener() {
		return new SolrUpdateEventHandler(solrClient, ops);
	}
}
