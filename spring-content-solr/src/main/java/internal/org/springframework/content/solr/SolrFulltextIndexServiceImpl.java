package internal.org.springframework.content.solr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.util.ContentStreamBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.solr.AttributeSyncer;
import org.springframework.content.solr.SolrProperties;

import static java.lang.String.format;
import static org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION.COMMIT;

public class SolrFulltextIndexServiceImpl implements IndexService {

    private final SolrClient solrClient;
    private final SolrProperties properties;
    private AttributeSyncer<Object> syncer;

    @Autowired
    public SolrFulltextIndexServiceImpl(SolrClient solrClient, SolrProperties properties) {
        this.solrClient = solrClient;
        this.properties = properties;
    }

    @Autowired(required=false)
    public void setAttributeSyncer(AttributeSyncer syncer) {
        this.syncer = syncer;
    }
    
    @Override
    public void index(Object entity, InputStream content) {

        ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
        if (properties.getUser() != null) {
            up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
        }

        up.addContentStream(new ContentEntityStream(content));
        String id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class).toString();
        up.setParam("literal.id", entity.getClass().getCanonicalName() + ":" + id);
        if (syncer != null) {
            Map<String,String> attributesToSync = syncer.synchronize(entity);
            for (Entry<String,String> entry : attributesToSync.entrySet()) {
                up.setParam(format("literal.%s", entry.getKey()), entry.getValue());
            }
        }
        up.setAction(COMMIT,true, true);

        try {
            solrClient.request(up, null);
        }
        catch (SolrServerException e) {
            throw new StoreAccessException(format("Error indexing entity with id '%s'", id), e);
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error indexing entity with id '%s'", id), e);
        }
    }

    @Override
    public void unindex(Object entity) {

        Object id = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);

        UpdateRequest up = new UpdateRequest();
        up.setAction(COMMIT,true, true);
        up.deleteById(entity.getClass().getCanonicalName() + ":" + id.toString());

        if (properties.getUser() != null) {
            up.setBasicAuthCredentials(properties.getUser(), properties.getPassword());
        }

        try {
            solrClient.request(up, null);
        }
        catch (SolrServerException e) {
            throw new StoreAccessException(format("Error unindexing entity with id '%s'", id), e);
        }
        catch (IOException e) {
            throw new StoreAccessException(format("Error unindexing entity with id '%s'", id), e);
        }
    }

    private class ContentEntityStream extends ContentStreamBase {

        private InputStream stream;

        public ContentEntityStream(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public InputStream getStream() throws IOException {
            return stream;
        }
    }
}
