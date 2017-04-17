package org.springframework.content.commons.repository;

import java.io.Serializable;

public interface AssociativeStore<S, SID extends Serializable> extends Store<SID> {
	
	void associate(SID id, S entity);
	void unassociate(S entity);

}
