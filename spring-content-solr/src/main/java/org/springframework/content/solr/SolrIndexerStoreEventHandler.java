package org.springframework.content.solr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

@StoreEventHandler
public class SolrIndexerStoreEventHandler {

	private final IndexService indexer;

	@Autowired
	public SolrIndexerStoreEventHandler(IndexService indexer) {
		Assert.notNull(indexer, "indexer must not be null");

		this.indexer = indexer;
	}

	@HandleAfterSetContent
	@Order(100)
	protected void onAfterSetContent(AfterSetContentEvent event) {

		Object contentEntity = event.getSource();
		if (BeanUtils.hasFieldWithAnnotation(contentEntity, ContentId.class) == false) {
			return;
		}

		if (BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class) == null) {
			return;
		}

		indexer.index(event.getSource(), event.getStore().getContent(event.getSource()));
	}

	@HandleBeforeUnsetContent
	@Order(100)
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {

		Object contentEntity = event.getSource();
		if (BeanUtils.hasFieldWithAnnotation(contentEntity, ContentId.class) == false) {
			return;
		}

		Object id = BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class);
		if (id == null) {
			return;
		}

		indexer.unindex(event.getSource());
	}
}
