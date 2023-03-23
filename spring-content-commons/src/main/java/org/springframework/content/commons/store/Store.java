package org.springframework.content.commons.store;

import org.springframework.core.io.Resource;

import java.io.Serializable;

public interface Store<SID extends Serializable> extends org.springframework.content.commons.repository.Store<SID> {
	/**
	 * Returns a resource handle for the specified id.
	 *
	 * @param id
	 * 			the id of the resource
	 * @return
	 * 			resource
	 */
	Resource getResource(SID id);
}