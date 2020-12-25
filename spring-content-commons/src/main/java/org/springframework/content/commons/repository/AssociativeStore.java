package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;

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
     * Returns the resource associated with the given property for entity, or null if no association exists
     *
     * @param entity
     *          the entity associated with resource
     * @param propertyPath
     *          the property path of the associated resource
     * @return  resource
     */
    Resource getResource(S entity, PropertyPath propertyPath);

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
     * Associates the resource (identified by id) with entity.
     *
     * @param entity
     *          the target of the association
     * @param propertyPath
     *          the property path to associate the resource to
     * @param id
     *          the id of the resource to be associated
     */
    void associate(S entity, PropertyPath propertyPath, SID id);

	/**
	 * Unassociates the resource from entity
	 *
	 * @param entity
	 * 			the target of the unassociation
	 */
	void unassociate(S entity);

    /**
     * Unassociates the resource from entity
     *
     * @param entity
     *          the target of the unassociation
     * @param propertyPath
     *          the property path to unassociate the resource from
     */
    void unassociate(S entity, PropertyPath propertyPath);
}
