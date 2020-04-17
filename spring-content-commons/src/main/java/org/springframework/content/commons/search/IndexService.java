package org.springframework.content.commons.search;

import java.io.InputStream;

public interface IndexService<T> {

    void index(T entity, InputStream content);

    void unindex(T entity);
}
