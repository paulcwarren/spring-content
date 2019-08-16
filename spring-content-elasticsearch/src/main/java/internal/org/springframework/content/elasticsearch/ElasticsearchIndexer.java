package internal.org.springframework.content.elasticsearch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.events.AbstractStoreEventListener;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.Assert;

import static java.lang.String.format;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

	public static final String INDEX_NAME = "spring-content-fulltext-index";

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexer.class);
	private static final String SPRING_CONTENT_ATTACHMENT = "spring-content-attachment-pipeline";
	private static final int BUFFER_SIZE = 3 * 1024;

	private RestHighLevelClient client;

	public ElasticsearchIndexer(RestHighLevelClient client) throws IOException {
		this.client = client;
		ensureAttachmentPipeline();
	}

	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		String id = BeanUtils.getFieldWithAnnotation(event.getSource(), ContentId.class).toString();
		InputStream stream = event.getStore().getContent(event.getSource());

		StringBuilder result = new StringBuilder();
		try {
			try (BufferedInputStream in = new BufferedInputStream(stream, BUFFER_SIZE); ) {
				Base64.Encoder encoder = Base64.getEncoder();
				byte[] chunk = new byte[BUFFER_SIZE];
				int len = 0;
				while ( (len = in.read(chunk)) == BUFFER_SIZE ) {
					result.append( encoder.encodeToString(chunk) );
				}
				if ( len > 0 ) {
					chunk = Arrays.copyOf(chunk,len);
					result.append( encoder.encodeToString(chunk) );
				}
			}
		}
		catch (IOException e) {
			throw new StoreAccessException(format("Error base64 encoding stream for content %s", id), e);
		}

		IndexRequest req = new IndexRequest(INDEX_NAME, event.getSource().getClass().getName(), id);
		req.setPipeline(SPRING_CONTENT_ATTACHMENT);

		String source = "{" +
				"\"data\": \"" + result.toString() + "\"" +
				"}";
		req.source(source, XContentType.JSON);

		try {
			IndexResponse res = client.index(req, RequestOptions.DEFAULT);
			LOGGER.info(format("Content '%s' indexed with result %s", id, res.getResult()));
		}
		catch (IOException e) {
			throw new StoreAccessException(format("Error indexing content %s", id), e);
		}
	}

	@Override
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
		Object id = BeanUtils.getFieldWithAnnotation(event.getSource(), ContentId.class);
		if (id == null) {
			return;
		}
		DeleteRequest req = new DeleteRequest(INDEX_NAME, event.getSource().getClass().getName(), id.toString());
		try {
			DeleteResponse res = client.delete(req, RequestOptions.DEFAULT);
			LOGGER.info(format("Indexed content '%s' deleted with result %s", id, res.getResult()));
		}
		catch (IOException e) {
			throw new StoreAccessException(format("Error deleting indexed content %s", id), e);
		}
	}

	void ensureAttachmentPipeline() throws IOException {
		GetPipelineRequest getRequest = new GetPipelineRequest(SPRING_CONTENT_ATTACHMENT);
		GetPipelineResponse res = client.ingest().getPipeline(getRequest, RequestOptions.DEFAULT);
		if (!res.isFound()) {
			String source = "{\"description\":\"Extract attachment information encoded in Base64 with UTF-8 charset\"," +
					"\"processors\":[{\"attachment\":{\"field\":\"data\"}}]}";
			PutPipelineRequest put = new PutPipelineRequest(SPRING_CONTENT_ATTACHMENT,
					new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
					XContentType.JSON);
			AcknowledgedResponse wpr = client.ingest().putPipeline(put, RequestOptions.DEFAULT);
			Assert.isTrue(wpr.isAcknowledged(), "Attachment pipeline not acknowledged by server");
		}
	}
}