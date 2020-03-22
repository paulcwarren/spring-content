package org.springframework.content.elasticsearch;

import java.io.IOException;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.search.IndexService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.springframework.content.elasticsearch" })
public class ElasticsearchFulltextIndexingConfig {

	@Autowired
	private RestHighLevelClient client;

	@Autowired(required=false)
	private RenditionService renditionService;

	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener() throws IOException {
		return new ElasticsearchIndexer(client, elasticFulltextIndexService());
	}

	@Bean
	public IndexService elasticFulltextIndexService() throws IOException {
		return new ElasticsearchIndexServiceImpl(client, renditionService, indexManager());
	}

	@Bean
	public IndexManager indexManager() {
		return new IndexManager(client);
	}
}
