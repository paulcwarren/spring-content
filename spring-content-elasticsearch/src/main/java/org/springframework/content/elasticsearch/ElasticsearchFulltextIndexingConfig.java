package org.springframework.content.elasticsearch;

import java.io.IOException;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionService;
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

// TODO: figure out who/what does the conversion from string IDs to idClass IDs
//	@Autowired
//	private ConversionService contentConversionService;

	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener() throws IOException {
		return new ElasticsearchIndexer(client, renditionService);
	}
}
