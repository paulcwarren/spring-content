package org.springframework.content.commons.operations;

import org.springframework.core.io.Resource;

public interface ResourceOperations {

    Resource get(String location);
    void delete(Resource resource);

}
