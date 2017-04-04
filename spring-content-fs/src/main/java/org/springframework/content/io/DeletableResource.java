package org.springframework.content.io;

import org.springframework.core.io.Resource;

public interface DeletableResource extends Resource {
	void delete();
}
