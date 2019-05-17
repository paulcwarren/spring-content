package org.springframework.content.elasticsearch;

import java.io.IOException;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@ComponentScan(basePackages = { "org.springframework.content.elasticsearch" })
public class FullTextIndexingElasticConfig {

	@Autowired
	private RestHighLevelClient client;

	@Autowired
	private ConversionService contentConversionService;

	@Bean
	public StoreExtension elasticFulltextSearcher() {
		return new ElasticsearchStoreExtension(client, new ReflectionServiceImpl(), contentConversionService);
	}

	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener() throws IOException {
		return new ElasticsearchIndexer(client);
	}
}
