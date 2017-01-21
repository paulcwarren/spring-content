package org.springframework.content.solr;

import internal.org.springframework.content.commons.utils.ReflectionService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.content.commons.repository.ContentAccessException;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentRepositoryInvoker;
import org.springframework.content.commons.search.Searchable;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

public class SolrSearchImpl implements Searchable<Object>, ContentRepositoryExtension {

    private SolrClient solr;
    private ReflectionService reflectionService;
    private ConversionService conversionService;
    private SolrProperties solrProperties;
    private String field = "id";

    public SolrSearchImpl(SolrClient solr, ReflectionService reflectionService, ConversionService conversionService, SolrProperties properties) {
        this.solr = solr;
        this.reflectionService = reflectionService;
        this.conversionService = conversionService;
        this.solrProperties = solrProperties;
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

    @Override
    public Set<Method> getMethods() {
        Set<Method> methods = new HashSet<>();
        methods.addAll(Arrays.asList(Searchable.class.getMethods()));
        return methods;
    }

    @Override
    public Object invoke(MethodInvocation invocation, ContentRepositoryInvoker invoker) {
        List newList = new ArrayList();
        Class<? extends Serializable> clazz = invoker.getContentIdClass();

        List<String> list = (List) reflectionService.invokeMethod(invocation.getMethod(), this, invocation.getArguments());
        for (String item : list) {
            if (conversionService.canConvert(item.getClass(), clazz) == false) {
                throw new IllegalStateException(String.format("Cannot convert item of type %s to %s", item.getClass().getName(), clazz.getName()));
            }
            newList.add(conversionService.convert(item, clazz));
        }

        return newList;
    }
}



