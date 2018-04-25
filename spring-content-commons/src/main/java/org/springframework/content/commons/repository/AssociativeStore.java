package org.springframework.content.commons.repository;

import org.springframework.core.io.Resource;

import java.io.Serializable;

public interface AssociativeStore<S, SID extends Serializable> extends Store<SID> {

	Resource getResource(S entity);

	void associate(S entity, SID id);
	void unassociate(S entity);

}
