package org.springframework.content.fs.io;

import org.springframework.core.io.Resource;

public interface DeletableResource extends Resource {
	void delete();
}
