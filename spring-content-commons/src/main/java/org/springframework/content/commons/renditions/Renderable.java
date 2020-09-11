package org.springframework.content.commons.renditions;

import java.io.InputStream;

public interface Renderable<S> {

    boolean hasRendition(S entity, String mimeType);

	InputStream getRendition(S entity, String mimeType);
}
