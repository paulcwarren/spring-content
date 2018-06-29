package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.core.io.Resource;

public interface Store<SID extends Serializable> {
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