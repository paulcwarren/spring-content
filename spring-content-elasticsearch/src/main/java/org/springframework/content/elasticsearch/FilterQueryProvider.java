package org.springframework.content.elasticsearch;

import java.util.Map;

public interface FilterQueryProvider {

    Map<String, Object> filterQueries(Class<?> entity);
}
