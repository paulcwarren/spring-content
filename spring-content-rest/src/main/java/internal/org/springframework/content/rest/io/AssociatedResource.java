package internal.org.springframework.content.rest.io;

import org.springframework.core.io.Resource;

public interface AssociatedResource<S> extends Resource {

    S getAssociation();

}
