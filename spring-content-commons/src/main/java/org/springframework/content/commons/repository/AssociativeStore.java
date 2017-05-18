package org.springframework.content.commons.repository;

import java.io.Serializable;

public interface AssociativeStore<S, SID extends Serializable> extends Store<SID> {
	
	void associate(S entity, SID id);
	void unassociate(S entity);

}
