package org.springframework.content.solr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.events.AbstractStoreEventListener;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

@StoreEventHandler
public class SolrIndexer {

	private final IndexService indexer;

	@Autowired
	public SolrIndexer(IndexService indexer) {
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
