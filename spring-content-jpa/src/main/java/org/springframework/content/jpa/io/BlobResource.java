package org.springframework.content.jpa.io;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

public interface BlobResource extends Resource, WritableResource, DeletableResource {

    Object getId();

}
