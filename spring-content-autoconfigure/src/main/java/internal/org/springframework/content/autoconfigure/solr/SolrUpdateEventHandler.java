package internal.org.springframework.content.autoconfigure.solr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentRepositoryEventHandler;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.repository.events.AbstractContentRepositoryEventListener;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.Assert;

@ContentRepositoryEventHandler
public class SolrUpdateEventHandler extends AbstractContentRepositoryEventListener<Object> {

	private SolrClient solrClient;
	
	@Autowired private ContentOperations ops;
	
	public SolrUpdateEventHandler(SolrClient solrCient) {
		this.solrClient = solrCient;
	}

	SolrUpdateEventHandler(SolrClient solrCient, ContentOperations ops) {
		this.solrClient = solrCient;
		this.ops = ops;
	}

	@Override
	protected void onAfterSetContent(Object contentEntity) {

	    ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
	    up.addContentStream(new ContentEntityStream(ops, contentEntity));
	    up.setParam("literal.id", BeanUtils.getFieldWithAnnotation(contentEntity, ContentId.class).toString());
	    up.setAction(org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION.COMMIT, true, true);
	    try {
			/*NamedList<Object> request = */solrClient.request(up, null);
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class ContentEntityStream extends ContentStreamBase {

		private ContentOperations ops;
		private Object contentEntity;
		
		public ContentEntityStream(ContentOperations ops, Object contentEntity) {
			Assert.notNull(ops, "ConentOperations cannot be null");
			Assert.notNull(contentEntity, "ContentEntity cannot be null");
			this.ops = ops;
			this.contentEntity = contentEntity;
		}
		
		@Override
		public InputStream getStream() throws IOException {
			return ops.getContent(contentEntity);
		}
		
	}
	
}
