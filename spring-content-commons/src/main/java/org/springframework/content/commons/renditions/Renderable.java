package org.springframework.content.commons.renditions;

import java.io.InputStream;

public interface Renderable<S> {

	InputStream getRendition(S entity, String mimeType);
}
