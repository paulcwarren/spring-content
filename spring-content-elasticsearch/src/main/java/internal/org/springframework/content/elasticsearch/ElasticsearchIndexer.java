package internal.org.springframework.content.elasticsearch;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.AbstractStoreEventListener;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

	public static final String INDEX_NAME = "spring-content-fulltext-index";

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexer.class);

	private final RestHighLevelClient client;
	private final IndexService indexService;

	public ElasticsearchIndexer(RestHighLevelClient client, IndexService indexService) throws IOException {
		this.client = client;
		this.indexService = indexService;
	}

	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		if (event.getStore() instanceof ContentStore) {
			this.indexService.index(event.getSource(), ((ContentStore)event.getStore()).getContent(event.getSource()));
		}
	}

	@Override
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
		this.indexService.unindex(event.getSource());
	}
}