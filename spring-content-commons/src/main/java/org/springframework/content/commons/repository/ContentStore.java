package org.springframework.content.commons.repository;

import org.springframework.versions.LockParticipant;

import java.io.InputStream;
import java.io.Serializable;

public interface ContentStore<S, SID extends Serializable>
		extends ContentRepository<S, SID> {

	@LockParticipant
	void setContent(S property, InputStream content);

	@LockParticipant
	void unsetContent(S property);

	InputStream getContent(S property);

}
