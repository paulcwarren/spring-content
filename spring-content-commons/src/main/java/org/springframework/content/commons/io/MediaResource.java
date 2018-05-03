package org.springframework.content.commons.io;

import org.springframework.core.io.Resource;

public interface MediaResource extends Resource {
	default String getMime() {
		return "application/octet-stream";
	}

	String getName();
}
