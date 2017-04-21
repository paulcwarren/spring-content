package org.springframework.content.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentRepositoryEventHandler;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.repository.ContentAccessException;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AbstractContentRepositoryEventListener;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.Assert;

@ContentRepositoryEventHandler
public class SolrIndexer extends AbstractContentRepositoryEventListener<Object> {

	private SolrClient solrClient;
	private ContentStore<Object,Serializable> store;
	private SolrProperties properties;
	
    @Autowired
    public SolrIndexer(SolrClient solrClient, SolrProperties properties) {
        Assert.notNull(solrClient, "solrClient must not be null");
        Assert.notNull(properties, "properties must not be null");

		this.solrClient = solrClient;
        this.properties = properties;
	}

	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		Object contentEntity = event.getSource();
		if (BeanUtils.hasFieldWithAnnotation(contentEntity, ContentId.class) == false) {
			return;
		}

		if (BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class) == null) {
			return;
		}

	    ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
		if (properties.getUser() != null) {
			up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
		}
		up.addContentStream(new ContentEntityStream(event.getStore().getContent(contentEntity)));
		String id = BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class).toString();
	    up.setParam("literal.id", contentEntity.getClass().getCanonicalName() + ":" + id);
	    up.setAction(org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION.COMMIT, true, true);
	    try {
			/*NamedList<Object> request = */solrClient.request(up, null);
		} catch (SolrServerException e) {
			throw new ContentAccessException(String.format("Error updating entry in solr index %s", id), e);
		} catch (IOException e) {
			throw new ContentAccessException(String.format("Error updating entry in solr index %s", id), e);
		}
	}
	
	@Override
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
		Object contentEntity = event.getSource();
		if (BeanUtils.hasFieldWithAnnotation(contentEntity, ContentId.class) == false) {
			return;
		}

		Object id = BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class);
		if (id == null) {
			return;
		}

		UpdateRequest up = new UpdateRequest();
		up.setAction(org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION.COMMIT, true, true);
		up.deleteById(contentEntity.getClass().getCanonicalName() + ":" + id.toString());
		if (properties.getUser() != null) {
            up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
        }
		try {
			solrClient.request(up, null);
		} catch (SolrServerException e) {
			throw new ContentAccessException(String.format("Error deleting entry from solr index %s", id.toString()), e);
		} catch (IOException e) {
			throw new ContentAccessException(String.format("Error deleting entry from solr index %s", id.toString()), e);
		}
	}

	public class ContentEntityStream extends ContentStreamBase {

		private InputStream stream;
		
		public ContentEntityStream(InputStream stream) {
			this.stream = stream;
		}
		
		public ContentEntityStream(ContentOperations ops, Object contentEntity) {
			Assert.notNull(stream, "stream cannot be null");
		}
		
		@Override
		public InputStream getStream() throws IOException {
			return stream;
		}
		
	}
	
}
