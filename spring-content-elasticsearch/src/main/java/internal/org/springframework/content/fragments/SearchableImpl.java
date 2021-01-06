package internal.org.springframework.content.fragments;

import static java.lang.String.format;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.elasticsearch.FilterQueryProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Pageable;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;

public class SearchableImpl implements Searchable<Object> {

    private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexer.class);

    private final RestHighLevelClient client;
    private final IndexManager manager;
    private FilterQueryProvider filterProvider;
    private ConversionService conversionService;

    private Class<?> domainClass;
    private Class<?> idClass;

    public SearchableImpl() {
        client = null;
        manager = null;
        domainClass = null;
        idClass = null;
        filterProvider = null;
    }

    @Autowired
    public SearchableImpl(RestHighLevelClient client, IndexManager manager) {
        this.client = client;
        this.manager = manager;
        this.filterProvider = null;
        this.conversionService = new DefaultConversionService();
    }

    @Autowired(required=false)
    public void setFilterQueryProvider(FilterQueryProvider provider) {
        this.filterProvider = provider;
    }

    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    public void setIdClass(Class<?> idClass) {
        this.idClass = idClass;
    }

    @Override
    public Iterable<Object> search(String queryStr) {
        return search(queryStr, null, idClass);
    }

    @Override
    public List<Object> search(String queryStr, Pageable pageable) {
        return search(queryStr, pageable, idClass);
    }

    @Override
    public List<Object> search(String queryString, Pageable pageable, Class<? extends Object> searchType) {

        SearchRequest searchRequest = new SearchRequest(manager.indexName(domainClass));
        searchRequest.types(domainClass.getName());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        List<String> attributesToFetch = new ArrayList<>();
        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
            for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(searchType, Attribute.class, new BeanWrapperImpl(searchType))) {
                Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                attributesToFetch.add(fieldAnnotation.name());
            }
        }
        if (attributesToFetch.size() > 0) {
            sourceBuilder.fetchSource(attributesToFetch.toArray(new String[]{}), null);
        }

        SimpleQueryStringBuilder sqsb = QueryBuilders.simpleQueryStringQuery(queryString);
        sqsb.field("attachment.content");

        BoolQueryBuilder b = QueryBuilders.boolQuery();
        b.must(sqsb);

        if (filterProvider != null) {
            Map<String,Object> filters = filterProvider.filterQueries(domainClass);
            for (String attr : filters.keySet()) {
                b.filter(QueryBuilders.matchQuery(attr, filters.get(attr)));
            }
        }

        sourceBuilder.query(b);
        if (pageable != null) {
            sourceBuilder.from(pageable.getPageNumber() * pageable.getPageSize());
            sourceBuilder.size(pageable.getPageSize());
        }

        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
            if (BeanUtils.findFieldWithAnnotation(searchType, Highlight.class) != null) {
                HighlightBuilder hb = SearchSourceBuilder.highlight();
                hb.field("attachment.content");
                sourceBuilder.highlighter(hb);
            }
        }

        searchRequest.source(sourceBuilder);

        SearchResponse res = null;
        try {
            res = client.search(searchRequest, RequestOptions.DEFAULT);
        }
        catch (IOException | ElasticsearchStatusException e) {
            LOGGER.error(format("Error searching indexed content for '%s'", queryString), e);
            throw new StoreAccessException(format("Error searching indexed content for '%s'", queryString), e);
        }

        return getResults(res.getHits(), searchType);
    }

    @Override
    public Iterable<Object> findKeyword(String query) {
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
    public Iterable<Object> findAllKeywords(String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> findAnyKeywords(String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> findKeywordsNear(int proximity, String... terms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> findKeywordStartsWith(String term) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        throw new UnsupportedOperationException();
    }

    private List<Object> getIDs(SearchHits result) {
        List<Object> contents = new ArrayList<>();

        if (result == null || result.getTotalHits().value == 0) {
            return contents;
        }

        for (SearchHit hit : result.getHits()) {
            contents.add(hit.getId());
        }

        return contents;
    }

    private List<Object> getResults(SearchHits result, Class<?> resultType) {

        List<Object> contents = new ArrayList<>();

        if (result == null || result.getTotalHits().value == 0) {
            return contents;
        }

        for (SearchHit hit : result.getHits()) {

            try {
                if (ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
                    contents.add(conversionService.convert(hit.getId(), TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(this.idClass)));
                } else {
                    Object row = resultType.newInstance();
                    BeanWrapper wrapper = new BeanWrapperImpl(row);


                    String id = hit.getId();

                    Field contentIdField = BeanUtils.findFieldWithAnnotation(resultType, ContentId.class);
                    if (contentIdField != null) {
                        wrapper.setPropertyValue(contentIdField.getName(), id);
                    }

                    Field highlightField = BeanUtils.findFieldWithAnnotation(resultType, Highlight.class);
                    if (highlightField != null) {
                        wrapper.setPropertyValue(highlightField.getName(), hit.getHighlightFields().get("attachment.content").getFragments()[0].string());
                    }

                    for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(resultType, Attribute.class, new BeanWrapperImpl(resultType))) {
                        Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                        wrapper.setPropertyValue(field.getName(), hit.getSourceAsMap().get(fieldAnnotation.name()));
                    }

                    contents.add(row);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return contents;
    }
}
