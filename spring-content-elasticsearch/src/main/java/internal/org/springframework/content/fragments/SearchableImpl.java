package internal.org.springframework.content.fragments;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.data.domain.Pageable;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;

public class SearchableImpl implements Searchable<Serializable> {

    private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexer.class);

    private final RestHighLevelClient client;
    private final IndexManager manager;

    private Class<?> domainClass;

    public SearchableImpl() {
        client = null;
        manager = null;
        domainClass = null;
    }

    @Autowired
    public SearchableImpl(RestHighLevelClient client, IndexManager manager) {
        this.client = client;
        this.manager = manager;
    }

    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public Iterable<Serializable> search(String queryStr) {
        SearchRequest searchRequest = new SearchRequest(manager.indexName(domainClass));
        searchRequest.types(domainClass.getName());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.simpleQueryStringQuery(queryStr));
        searchRequest.source(sourceBuilder);

        SearchResponse res = null;
        try {
            res = client.search(searchRequest, RequestOptions.DEFAULT);
        }
        catch (IOException | ElasticsearchStatusException e) {
            LOGGER.error(format("Error searching indexed content for '%s'", queryStr), e);
            throw new StoreAccessException(format("Error searching indexed content for '%s'", queryStr), e);
        }

        return getIDs(res.getHits());
    }

    @Override
    public List<Serializable> search(String queryStr, Pageable pageable) {
        SearchRequest searchRequest = new SearchRequest(manager.indexName(domainClass));
        searchRequest.types(domainClass.getName());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.simpleQueryStringQuery(queryStr));
        sourceBuilder.from(pageable.getPageNumber() * pageable.getPageSize());
        sourceBuilder.size(pageable.getPageSize());
        searchRequest.source(sourceBuilder);

        SearchResponse res = null;
        try {
            res = client.search(searchRequest, RequestOptions.DEFAULT);
        }
        catch (IOException | ElasticsearchStatusException e) {
            LOGGER.error(format("Error searching indexed content for '%s'", queryStr), e);
            throw new StoreAccessException(format("Error searching indexed content for '%s'", queryStr), e);
        }

        return getIDs(res.getHits());
    }

    @Override
    public List<Serializable> search(String queryString, Pageable pageable, Class<? extends Serializable> searchType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findKeyword(String query) {
        SearchRequest searchRequest = new SearchRequest(manager.indexName(domainClass));
        searchRequest.types(domainClass.getName());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.queryStringQuery(query));
        searchRequest.source(sourceBuilder);

        SearchResponse res = null;
        try {
            res = client.search(searchRequest, RequestOptions.DEFAULT);
        }
        catch (IOException ioe) {
            throw new StoreAccessException(format("Error searching indexed content for '%s'", query), ioe);
        }

        return getIDs(res.getHits());
    }

    @Override
    public Iterable<Serializable> findAllKeywords(String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findAnyKeywords(String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findKeywordsNear(int proximity, String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findKeywordStartsWith(String term) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findKeywordStartsWithAndEndsWith(String a, String b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Serializable> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        throw new UnsupportedOperationException();
    }

    private List<Serializable> getIDs(SearchHits result) {
        List<Serializable> contents = new ArrayList<>();

        if (result == null || result.getTotalHits().value == 0) {
            return contents;
        }

        for (SearchHit hit : result.getHits()) {
            contents.add(hit.getId());
        }

        return contents;
    }
}
