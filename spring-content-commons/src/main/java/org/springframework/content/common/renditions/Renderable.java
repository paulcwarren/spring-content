package org.springframework.content.common.renditions;

import java.io.InputStream;

public interface Renderable<S> {
	InputStream getRendition(S property, String mimeType);
}
