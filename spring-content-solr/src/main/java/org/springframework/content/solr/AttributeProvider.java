package org.springframework.content.solr;

import java.util.Map;

public interface AttributeProvider<S> {

    Map<String, String> synchronize(S entity);
}
