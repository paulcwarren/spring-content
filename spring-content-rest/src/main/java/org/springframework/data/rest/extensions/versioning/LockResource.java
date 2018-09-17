package org.springframework.data.rest.extensions.versioning;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.versions.Lock;

public class LockResource extends Resource<Lock> {

    public LockResource(Lock content, Link... links) {
        super(content, links);
    }

    public LockResource(Lock content, Iterable<Link> links) {
        super(content, links);
    }

}
