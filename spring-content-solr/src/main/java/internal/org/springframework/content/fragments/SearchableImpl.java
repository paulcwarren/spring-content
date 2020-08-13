package internal.org.springframework.content.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.solr.FilterQueryProvider;
import org.springframework.content.solr.SolrProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

public class SearchableImpl implements Searchable<Object> {

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
   
   public void setDomainClass(Class<?> domainClass) {
      this.domainClass = domainClass;
   }

   @Override
   public List<Object> search(String queryStr) {
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> search(String queryStr, Pageable pageable) {
      return getIds(executeQuery(this.getDomainClass(), queryStr, pageable));
   }

   @Override
   public List<Object> findKeyword(String queryStr) {
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findAllKeywords(String... terms) {
      String queryStr = this.parseTerms("AND", terms);
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findAnyKeywords(String... terms) {
      String queryStr = this.parseTerms("OR", terms);
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findKeywordsNear(int proximity, String... terms) {
      String termStr = this.parseTerms("NONE", terms);
      String queryStr = "\"" + termStr + "\"~" + Integer.toString(proximity);
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findKeywordStartsWith(String term) {
      String queryStr = term + "*";
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findKeywordStartsWithAndEndsWith(String a, String b) {
      String queryStr = a + "*" + b;
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
   }

   @Override
   public List<Object> findAllKeywordsWithWeights(String[] terms, double[] weights) {
      String queryStr = parseTermsAndWeights("AND", terms, weights);
      return getIds(executeQuery(this.getDomainClass(), queryStr, null));
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

   /* package */ List<Object> getIds(NamedList response) {
      List<Object> ids = new ArrayList<>();
      SolrDocumentList list = (SolrDocumentList) response.get("response");
      for (int j = 0; j < list.size(); ++j) {
         String id = list.get(j).getFieldValue("id").toString();
         id = id.substring(id.indexOf(':') + 1, id.length());
         ids.add(id);
      }

      return ids;
   }

   /* package */ QueryRequest solrAuthenticate(QueryRequest request) {
      request.setBasicAuthCredentials(solrProperties.getUser(),
            solrProperties.getPassword());
      return request;
   }

   /* package */ NamedList<Object> executeQuery(Class<?> domainClass, String queryString, Pageable pageable) {
      SolrQuery query = new SolrQuery();
      query.setQuery("(" + queryString + ") AND id:" + domainClass.getCanonicalName() + "\\:*");
      
      if (this.filterProvider != null) {
          query.setFilterQueries(this.filterProvider.filterQueries(domainClass));
      }
      
      query.setFields(field);

      if (pageable != null) {
         query.setStart(pageable.getPageNumber() * pageable.getPageSize());
         query.setRows(pageable.getPageSize());
      }

      QueryRequest request = new QueryRequest(query);
      if (solrProperties.getUser() != null) {
         request = solrAuthenticate(request);
      }

      NamedList<Object> response = null;
      try {
         response = solr.request(request, null);
      }
      catch (SolrServerException e) {
         throw new StoreAccessException(
               String.format("Error running query %s on field %s against solr.",
                     queryString, field),
               e);
      }
      catch (IOException e) {
         throw new StoreAccessException(
               String.format("Error running query %s on field %s against solr.",
                     queryString, field),
               e);
      }
      return response;
   }

   protected Class<?> getDomainClass() {
      return domainClass;
   }
}
