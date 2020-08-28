package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.solr.FilterQueryProvider;
import org.springframework.content.solr.Highlight;
import org.springframework.content.solr.SolrProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

public class SearchableImpl implements Searchable<Object>, ContentStoreAware {

    private static final String field = "id";

    private SolrClient solr;
    private SolrProperties solrProperties;
    private Class<?> domainClass;
    private FilterQueryProvider filterProvider;

    @Autowired
    public SearchableImpl(SolrClient solr, SolrProperties solrProperties) {
        this.solr = solr;
        this.solrProperties = solrProperties;
    }

    @Autowired(required=false)
    public void setFilterQueryProvider(FilterQueryProvider provider) {
        this.filterProvider = provider;
    }

    @Override
    public void setDomainClass(Class<?> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public List<Object> search(String queryStr) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> search(String queryStr, Pageable pageable) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, pageable, String.class), String.class);
    }

    @Override
    public List<Object> search(String queryStr, Pageable pageable, Class<?> searchType) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, pageable, searchType), searchType);
    }

    @Override
    public List<Object> findKeyword(String queryStr) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findAllKeywords(String... terms) {
        String queryStr = this.parseTerms("AND", terms);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findAnyKeywords(String... terms) {
        String queryStr = this.parseTerms("OR", terms);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findKeywordsNear(int proximity, String... terms) {
        String termStr = this.parseTerms("NONE", terms);
        String queryStr = "\"" + termStr + "\"~" + Integer.toString(proximity);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findKeywordStartsWith(String term) {
        String queryStr = term + "*";
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
        String queryStr = a + "*" + b;
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    @Override
    public List<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        String queryStr = parseTermsAndWeights("AND", terms, weights);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, String.class), String.class);
    }

    /* package */ String parseTermsAndWeights(String operator, String[] terms,
            double[] weights) {
        Assert.state(terms.length == weights.length, "all terms must have a weight");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < terms.length - 1; i++) {
            builder.append("(");
            builder.append(terms[i]);
            builder.append(")^");
            builder.append(weights[i]);
            builder.append(" " + operator + " ");
        }
        builder.append("(");
        builder.append(terms[terms.length - 1]);
        builder.append(")^");
        builder.append(weights[weights.length - 1]);

        return builder.toString();
    }

    /* package */ String parseTerms(String operator, String... terms) {
        String separator;

        if (operator == "NONE") {
            separator = " ";
        }
        else {
            separator = " " + operator + " ";
        }
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < terms.length - 1; i++) {
            builder.append(terms[i]);
            builder.append(separator);
        }
        builder.append(terms[terms.length - 1]);
        return builder.toString();
    }

    /* package */ QueryRequest solrAuthenticate(QueryRequest request) {
        request.setBasicAuthCredentials(solrProperties.getUser(),
                solrProperties.getPassword());
        return request;
    }

    /* package */ QueryResponse executeQuery(Class<?> domainClass, String queryString, Pageable pageable, Class<?> resultType) {
        SolrQuery query = new SolrQuery();
        query.setQuery("_text_:" + queryString);

        query.addFilterQuery("id:" + domainClass.getCanonicalName() + "\\:*");

        if (this.filterProvider != null) {
            query.addFilterQuery(this.filterProvider.filterQueries(domainClass));
        }

        query.setFields(field);

        if (BeanUtils.findFieldWithAnnotation(resultType, Highlight.class) != null) {
            query.setHighlight(true);
        }

        if (pageable != null) {
            query.setStart(pageable.getPageNumber()
                    * pageable.getPageSize());
            query.setRows(pageable.getPageSize());
        }

        QueryRequest request = new QueryRequest(query);
        if (solrProperties.getUser() != null) {
            request = solrAuthenticate(request);
        }

        QueryResponse response = null;
        try {
            response = request.process(solr, null);
        } catch (SolrServerException e) {
            throw new StoreAccessException(String.format("Error running query %s on field %s against solr.", queryString, field), e);
        } catch (IOException e) {
            throw new StoreAccessException(String.format("Error running query %s on field %s against solr.", queryString, field), e);
        }

        return response;
    }

    private List<Object> getResults(QueryResponse response, Class<?> searchType) {

        List<Object> results = new ArrayList<>();
        SolrDocumentList list = response.getResults();
        for (int j = 0; j < list.size(); ++j) {

            String id = list.get(j).getFieldValue("id").toString();
            String strippedId = id.substring(id.indexOf(':') + 1, id.length());

            if (searchType.isPrimitive() || String.class.equals(searchType)) {
                results.add(strippedId);
            } else {
                Object result = null;
                try {
                    result = searchType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (BeanUtils.findFieldWithAnnotation(searchType, ContentId.class) != null) {
                    BeanUtils.setFieldWithAnnotation(result, ContentId.class, strippedId);
                }

                if (BeanUtils.findFieldWithAnnotation(searchType, Highlight.class) != null) {
                    Map<String, Map<String, List<String>>> highlights = response.getHighlighting();
                    List<String> highlight = highlights.get(id).get("_text_");
                    BeanUtils.setFieldWithAnnotation(result, Highlight.class, highlight.get(0));
                }
                results.add(result);
            }
        }

        return results;
    }

    protected Class<?> getDomainClass() {
        return domainClass;
    }

    @Override
    public void setIdClass(Class<?> idClass) {
    }

    @Override
    public void setContentStore(ContentStore store) {
    }
}
