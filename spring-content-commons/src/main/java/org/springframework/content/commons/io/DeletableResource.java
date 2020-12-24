package org.springframework.content.commons.io;

import java.io.IOException;

import org.springframework.core.io.Resource;

public interface DeletableResource extends Resource {

	/**
	 * Deletes the resource. <br>
	 * <br>
	 * Returns true if the deletion was successful, otherwise false. If the operation
	 * returns true then the resource handle itself must be considered unreliable and
	 * should be discarded.
	 * throws IOException when the resource cant be deleted
	 */
	void delete() throws IOException;

}
