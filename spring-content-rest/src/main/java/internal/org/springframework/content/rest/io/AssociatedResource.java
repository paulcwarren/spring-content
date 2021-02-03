package internal.org.springframework.content.rest.io;

import org.springframework.core.io.WritableResource;

public interface AssociatedResource<S> extends WritableResource, StoreResource {

    S getAssociation();
}
