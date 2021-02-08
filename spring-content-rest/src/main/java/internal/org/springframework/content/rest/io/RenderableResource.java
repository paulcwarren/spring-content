package internal.org.springframework.content.rest.io;

import java.io.InputStream;

import org.springframework.util.MimeType;

public interface RenderableResource {

    boolean isRenderableAs(MimeType mimeType);

    InputStream renderAs(MimeType mimeType);
}
