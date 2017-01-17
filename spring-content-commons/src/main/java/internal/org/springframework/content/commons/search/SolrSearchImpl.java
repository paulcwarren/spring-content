package internal.org.springframework.content.commons.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.content.commons.repository.ContentAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolrSearchImpl implements Searchable<Object> {

    private SolrClient solr;
    private String field = "id";

    public SolrSearchImpl(SolrClient solr) {
        this.solr = solr;
    }

    @Override
    public List<Object> findKeyword(String queryStr) {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findAllKeywords(String... terms) {
        SolrQuery query = new SolrQuery();
        String queryStr = this.parseTerms("AND", terms);
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findAnyKeywords(String... terms) {
        SolrQuery query = new SolrQuery();
        String queryStr =  this.parseTerms("OR", terms);
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findKeywordsNear(int proximity, String... terms) {
        SolrQuery query = new SolrQuery();
        String termStr = this.parseTerms("NONE", terms);
        String queryStr = "\""+ termStr + "\"~"+ Integer.toString(proximity);
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findKeywordStartsWith(String term) {
        SolrQuery query = new SolrQuery();
        String queryStr = term + "*";
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
        SolrQuery query = new SolrQuery();
        String queryStr = a + "*" + b;
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    @Override
    public List<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        SolrQuery query = new SolrQuery();
        String queryStr = parseTermsAndWeights("AND", terms, weights);
        query.setQuery(queryStr);
        query.setFields(field);

        QueryResponse response = null;
        try {
            response = solr.query(query);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryStr, field), e);
        }
        return getIds(response);
    }

    /* package */ String parseTermsAndWeights(String operator, String[] terms, double[] weights){
        Assert.state(terms.length == weights.length, "all terms must have a weight");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < terms.length - 1; i++){
            builder.append("(");
            builder.append(terms[i]);
            builder.append(")^");
            builder.append(weights[i]);
            builder.append(" " + operator + " ");
        }
        builder.append("(");
        builder.append(terms[terms.length-1]);
        builder.append(")^");
        builder.append(weights[weights.length-1]);

        return builder.toString();
    }

    /* package */ String parseTerms(String operator, String... terms){
        String separator;

        if(operator == "NONE") {
            separator = " ";
        }
        else {
            separator = " " + operator + " ";
        }
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < terms.length - 1; i++){
            builder.append(terms[i]);
            builder.append(separator);
        }
        builder.append(terms[terms.length - 1]);
        return builder.toString();
    }

    /* package */ List<Object> getIds(QueryResponse response) {
        List<Object> ids = new ArrayList<>();
        SolrDocumentList results = response.getResults();
        for (int i = 0; i < results.size(); ++i) {
            ids.add(results.get(i).getFieldValue("id"));
        }
        return ids;
    }
}



