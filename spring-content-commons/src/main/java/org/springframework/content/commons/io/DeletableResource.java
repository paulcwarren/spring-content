package org.springframework.content.commons.io;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface DeletableResource extends Resource {

	/**
	 * Deletes the resource. <br>
	 * <br>
	 * Returns true if the deletion was successful, otherwise false. If the operation
	 * returns true then the resource handle itself must be considered unreliable and
	 * should be discarded.
	 */
	void delete() throws IOException;

}
