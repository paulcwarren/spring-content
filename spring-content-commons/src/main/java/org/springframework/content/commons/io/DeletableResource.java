package org.springframework.content.commons.io;

import org.springframework.core.io.Resource;

public interface DeletableResource extends Resource {
	void delete();
}
