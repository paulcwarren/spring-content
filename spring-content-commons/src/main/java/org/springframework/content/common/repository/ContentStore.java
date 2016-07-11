package org.springframework.content.common.repository;

import java.io.InputStream;
import java.io.Serializable;

public interface ContentStore<S, SID extends Serializable> extends ContentRepository<S, SID> {
	
	void setContent(S property, InputStream content);
	void unsetContent(S property);
	InputStream getContent(S property);

}
