package org.springframework.content.elasticsearch;

import java.util.Map;

public interface AttributeProvider<S> {

    Map<String, String> synchronize(S entity);
}
