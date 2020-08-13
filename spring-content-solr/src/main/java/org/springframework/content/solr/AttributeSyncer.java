package org.springframework.content.solr;

import java.util.Map;

public interface AttributeSyncer<S> {

    Map<String, String> synchronize(S entity);
}
