package org.springframework.content.solr;

import org.springframework.content.commons.utils.ReflectionService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
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

    public SolrSearchImpl(SolrClient solr, ReflectionService reflectionService, ConversionService conversionService, SolrProperties solrProperties) {
        this.solr = solr;
        this.reflectionService = reflectionService;
        this.conversionService = conversionService;
        this.solrProperties = solrProperties;
    }

    @Override
    public List<Object> findKeyword(String queryStr) {
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findAllKeywords(String... terms) {
        String queryStr = this.parseTerms("AND", terms);
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findAnyKeywords(String... terms) {
        String queryStr =  this.parseTerms("OR", terms);
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findKeywordsNear(int proximity, String... terms) {
        String termStr = this.parseTerms("NONE", terms);
        String queryStr = "\""+ termStr + "\"~"+ Integer.toString(proximity);
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findKeywordStartsWith(String term) {
        String queryStr = term + "*";
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
        String queryStr = a + "*" + b;
        return getIds(executeQuery(queryStr));
    }

    @Override
    public List<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        String queryStr = parseTermsAndWeights("AND", terms, weights);
        return getIds(executeQuery(queryStr));
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

    /* package */ List<Object> getIds(NamedList response) {
        List<Object> ids = new ArrayList<>();
        for (int i = 1; i < response.size(); i++) {
            SolrDocumentList list = (SolrDocumentList) response.getVal(i);
            for (int j = 0; j < list.size(); ++j) {
                ids.add(list.get(j).getFieldValue("id"));
            }
        }
        return ids;
    }

    /* package */ QueryRequest solrAuthenticate(QueryRequest request) {
        request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
        return request;
    }

    /* package */ NamedList<Object> executeQuery(String queryString) {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        query.setFields(field);
        QueryRequest request = new QueryRequest(query);
        if (!solrProperties.getUser().isEmpty()) {
            request = solrAuthenticate(request);
        }
        NamedList<Object> response = null;

        try {
            response = solr.request(request, null);
        } catch (SolrServerException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryString, field), e);
        } catch (IOException e) {
            throw new ContentAccessException(String.format("Error running query %s on field %s against solr.", queryString, field), e);
        }
        return response;
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





