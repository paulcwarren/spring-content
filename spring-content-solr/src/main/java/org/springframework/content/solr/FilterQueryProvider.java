package org.springframework.content.solr;

public interface FilterQueryProvider {
    
    String[] filterQueries(Class<?> entity);
}
