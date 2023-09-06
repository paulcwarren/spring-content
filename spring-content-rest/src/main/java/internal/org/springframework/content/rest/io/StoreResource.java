package internal.org.springframework.content.rest.io;

import java.io.InputStream;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

public interface StoreResource extends WritableResource, DeletableResource {

    StoreInfo getStoreInfo();

    Object getETag();

    MediaType getMimeType();

    boolean isRenderableAs(MimeType mimeType);

    InputStream renderAs(MimeType mimeType);
}
