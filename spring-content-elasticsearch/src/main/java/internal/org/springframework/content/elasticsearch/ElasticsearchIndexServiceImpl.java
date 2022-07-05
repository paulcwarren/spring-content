package internal.org.springframework.content.elasticsearch;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchStatusException;
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
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.elasticsearch.AttributeProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ElasticsearchIndexServiceImpl<T> implements IndexService<T> {

    // if original index exists, use it, otherwise use class-based index

    private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexServiceImpl.class);
    private static final String SPRING_CONTENT_ATTACHMENT = "spring-content-attachment-pipeline";
    private static final int BUFFER_SIZE = 3 * 1024;

    private final RestHighLevelClient client;
    private final RenditionService renditionService;
    private final IndexManager manager;
    private final AttributeProvider attributeProvider;
    private final ObjectMapper objectMapper;

    private boolean pipelinedInitialized = false;

    public ElasticsearchIndexServiceImpl(RestHighLevelClient client, RenditionService renditionService, IndexManager manager, AttributeProvider attributeProvider) {

        this.client = client;
        this.renditionService = renditionService;
        this.manager = manager;
        this.attributeProvider = attributeProvider;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void index(T entity, InputStream stream) {

        if (!pipelinedInitialized) {
            try {
                ensureAttachmentPipeline();
            } catch (IOException ioe) {
                throw new StoreAccessException("Unable to initialize attachment pipeline", ioe);
            }
        }

        String id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class).toString();

        if (renditionService != null) {
            Object mimeType = BeanUtils.getFieldWithAnnotation(entity, MimeType.class);
            if (mimeType != null) {
                String strMimeType = mimeType.toString();
                if (renditionService.canConvert(strMimeType, "text/plain")) {
                    stream = renditionService.convert(strMimeType, stream, "text/plain");
                }
            }
        }

        StringBuilder result = new StringBuilder();
        try {
            try (BufferedInputStream in = new BufferedInputStream(stream, BUFFER_SIZE)) {
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

        IndexRequest req = new IndexRequest(manager.indexName(entity.getClass()), entity.getClass().getName(), id);
        req.setPipeline(SPRING_CONTENT_ATTACHMENT);

        Map<String, String> attributesToSync = new HashMap<>();
        if (attributeProvider != null) {
            attributesToSync = attributeProvider.synchronize(entity);
        }

        attributesToSync.put("data", result.toString());

        String payload = "";
        try {
            payload = objectMapper.writeValueAsString(attributesToSync);
        } catch (JsonProcessingException e) {
            throw new StoreAccessException(format("Unable to serialize payload for content %s", id), e);
        }

        req.source(payload, XContentType.JSON);

        try {
            IndexResponse res = client.index(req, RequestOptions.DEFAULT);
            LOGGER.info(format("Content '%s' indexed with result %s", id, res.getResult()));
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error indexing content %s", id), e);
        }
    }

    @Override
    public void unindex(T entity) {

        if (!pipelinedInitialized) {
            try {
                ensureAttachmentPipeline();
            } catch (IOException ioe) {
                throw new StoreAccessException("Unable to initialize attachment pipeline", ioe);
            }
        }

        Object id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (id == null) {
            return;
        }

        DeleteRequest req = new DeleteRequest(manager.indexName(entity.getClass()), entity.getClass().getName(), id.toString());
        try {
            DeleteResponse res = client.delete(req, RequestOptions.DEFAULT);
            LOGGER.info(format("Indexed content '%s' deleted with result %s", id, res.getResult()));
        }
        catch (ElasticsearchStatusException ese) {
            if (ese.status() != RestStatus.NOT_FOUND) {
                // TODO: re-throw as StoreIndexException
            }
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
