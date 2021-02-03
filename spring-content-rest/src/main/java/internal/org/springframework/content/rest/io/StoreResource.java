package internal.org.springframework.content.rest.io;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.MediaType;

public interface StoreResource extends WritableResource, DeletableResource {

    Object getETag();

    MediaType getMimeType();
}
