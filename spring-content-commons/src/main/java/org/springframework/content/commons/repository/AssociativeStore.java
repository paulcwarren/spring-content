package org.springframework.content.commons.repository;

import org.springframework.core.io.Resource;

import java.io.Serializable;

public interface AssociativeStore<S, SID extends Serializable> extends Store<SID> {

	/**
	 * Returns the resource associated with the given entity, or null if no association exists
	 *
	 * @param entity
	 * 			the entity associated with resource
	 * @return	resource
	 */
	Resource getResource(S entity);

	/**
	 * Associates the resource (identified by id) with entity.
	 *
	 * @param entity
	 * 			the target of the association
	 * @param id
	 * 			the id of the resource to be associated
	 */
	void associate(S entity, SID id);

	/**
	 * Unassociates the resource from entity
	 *
	 * @param entity
	 * 			the target of the unassociation
	 */
	void unassociate(S entity);

}
