package org.springframework.content.commons.repository;

import java.io.Serializable;

import org.springframework.core.io.Resource;

public interface Store<SID extends Serializable> {

	Resource getResource(SID id);
	
}
