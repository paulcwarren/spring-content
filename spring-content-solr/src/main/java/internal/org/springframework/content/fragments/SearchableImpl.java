package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.commons.utils.DomainObjectUtils;
import org.springframework.content.solr.FilterQueryProvider;
import org.springframework.content.solr.SolrProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;

public class SearchableImpl implements Searchable<Object>, ContentStoreAware {

    private static final String field = "id";

    private SolrClient solr;
    private SolrProperties solrProperties;
    private Class<?> domainClass;
    private Class<?> idClass;
    private Class<?>[] genericArguments;
    private FilterQueryProvider filterProvider;
    private ConversionService conversionService;

    @Autowired
    public SearchableImpl(SolrClient solr, SolrProperties solrProperties) {
        this.solr = solr;
        this.solrProperties = solrProperties;
        this.conversionService = new DefaultConversionService();
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
    public void setIdClass(Class<?> idClass) {
        this.idClass = idClass;
    }

    public void setGenericArguments(Class<?>[] genericArguments) {
        this.genericArguments = genericArguments;
    }

    @Override
    public List<Object> search(String queryStr) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public Page<Object> search(String queryStr, Pageable pageable) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, pageable, genericArguments[0]), pageable, genericArguments[0], PageImpl.class);
    }

    @Override
    public List<Object> findKeyword(String queryStr) {
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findAllKeywords(String... terms) {
        String queryStr = this.parseTerms("AND", terms);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findAnyKeywords(String... terms) {
        String queryStr = this.parseTerms("OR", terms);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findKeywordsNear(int proximity, String... terms) {
        String termStr = this.parseTerms("NONE", terms);
        String queryStr = "\"" + termStr + "\"~" + Integer.toString(proximity);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findKeywordStartsWith(String term) {
        String queryStr = term + "*";
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
        String queryStr = a + "*" + b;
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
    }

    @Override
    public List<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
        String queryStr = parseTermsAndWeights("AND", terms, weights);
        return getResults(executeQuery(this.getDomainClass(), queryStr, null, genericArguments[0]), null, genericArguments[0], ArrayList.class);
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

        java.lang.reflect.Field idField = DomainObjectUtils.getIdField(domainClass);
        if (idField != null) {
            query.addField(SolrFulltextIndexServiceImpl.ENTITY_ID);
        }

        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
            for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(resultType, Attribute.class, new BeanWrapperImpl(resultType))) {
                Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                query.addField(fieldAnnotation.name());
            }
        }

        if (!ContentPropertyUtils.isPrimitiveContentPropertyClass(resultType)) {
            if (BeanUtils.findFieldWithAnnotation(resultType, Highlight.class) != null) {
                query.setHighlight(true);
            }
        }

        if (pageable != null) {
            query.setStart(pageable.getPageNumber() * pageable.getPageSize());
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

    private <T> T getResults(QueryResponse response, Pageable pageable, Class<?> searchType, Class<T> returnType) {

        List<Object> results = new ArrayList<>();

        SolrDocumentList list = response.getResults();

        if (results == null || list.size() == 0) {
            return wrapResult(returnType, results, pageable, 0);
        }

        for (int j = 0; j < list.size(); ++j) {

            String id = list.get(j).getFieldValue("id").toString();
            String strippedId = id.substring(id.indexOf(':') + 1, id.length());

            if (ContentPropertyUtils.isPrimitiveContentPropertyClass(searchType)) {
                results.add(conversionService.convert(strippedId, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(idClass)));
            } else {
                Object result = null;
                try {
                    result = searchType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                Field idField = DomainObjectUtils.getIdField(searchType);
                if (idField != null) {
                    new BeanWrapperImpl(result).setPropertyValue(idField.getName(), list.get(j).getFirstValue(SolrFulltextIndexServiceImpl.ENTITY_ID));
                }

                if (BeanUtils.findFieldWithAnnotation(searchType, ContentId.class) != null) {
                    BeanUtils.setFieldWithAnnotation(result, ContentId.class, strippedId);
                }

                if (BeanUtils.findFieldWithAnnotation(searchType, Highlight.class) != null) {
                    Map<String, Map<String, List<String>>> highlights = response.getHighlighting();
                    List<String> highlight = highlights.get(id).get("_text_");
                    BeanUtils.setFieldWithAnnotation(result, Highlight.class, highlight.get(0));
                }

                for (java.lang.reflect.Field field : BeanUtils.findFieldsWithAnnotation(searchType, Attribute.class, new BeanWrapperImpl(searchType))) {
                    Attribute fieldAnnotation = field.getAnnotation(Attribute.class);
                    if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                        new BeanWrapperImpl(result).setPropertyValue(field.getName(), list.get(j).getFirstValue(fieldAnnotation.name()));
                    } else {
                        new BeanWrapperImpl(result).setPropertyValue(field.getName(), list.get(j).getFieldValues(fieldAnnotation.name()));
                    }
                }

                results.add(result);
            }
        }

        return wrapResult(returnType, results, pageable, list.size());
    }

    protected Class<?> getDomainClass() {
        return domainClass;
    }

    @Override
    public void setContentStore(ContentStore store) {
    }

    @Override
    public void setContentStore(org.springframework.content.commons.store.ContentStore store) {
    }


    @SuppressWarnings("unchecked")
    private <T> T wrapResult(Class<T> returnType, List<Object> content, Pageable pageable, long total) {

        T rc = null;
        if (Page.class.isAssignableFrom(returnType)) {
            rc = (T) new PageImpl<Object>(content, pageable, total);
        } else {
            rc = (T) content;
        }
        return rc;
    }
}
