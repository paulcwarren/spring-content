package internal.org.springframework.content.solr;

import java.io.InputStream;

import org.springframework.content.commons.search.IndexService;

public class SolrFulltextIndexServiceImpl implements IndexService {
    @Override
    public void index(Object entity, InputStream content) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unindex(Object entity) {
        throw new UnsupportedOperationException("not implemented");
    }
}
