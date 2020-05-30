package internal.org.springframework.content.elasticsearch.boot.autoconfigure;

import java.io.IOException;
import internal.org.springframework.content.elasticsearch.ElasticsearchConfig;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Configuration
@ConditionalOnClass({RestHighLevelClient.class, EnableElasticsearchFulltextIndexing.class})
@Import(ElasticsearchConfig.class)
public class ElasticsearchAutoConfiguration {

	// optional (based on properties)
	@ConditionalOnProperty(prefix="spring.content.elasticsearch", name="autoindex", havingValue="true", matchIfMissing = true)
	@ConditionalOnMissingBean(ElasticsearchIndexer.class)
	@Bean
	public ElasticsearchIndexer elasticFulltextIndexerEventListener(RestHighLevelClient client, IndexService elasticFulltextIndexService) throws IOException {
		return new ElasticsearchIndexer(client, elasticFulltextIndexService);
	}

	// user supplied
	@Bean
	@ConditionalOnMissingBean(RestHighLevelClient.class)
	public RestHighLevelClient restHighLevelClient() {
		return new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
	}

	@Component
	@ConfigurationProperties(prefix = "spring.content.elasticsearch")
	public static class ElasticsearchProperties {

        /**
         * Whether or not to perform automatic indexing of content as it is added
         */
        boolean autoindex = true;

		public boolean getAutoindex() {
			return autoindex;
		}

		public void setAutoindex(boolean autoindex) {
			this.autoindex = autoindex;
		}
	}
}
