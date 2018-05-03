package org.springframework.content.commons.renditions;

import org.springframework.core.io.Resource;

public interface Renderable<S> {
	Resource getRendition(S property, String mimeType);
}
