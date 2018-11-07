package org.springframework.content.commons.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.versions.LockParticipant;

import java.io.InputStream;
import java.io.Serializable;

@Transactional
public interface ContentStore<S, SID extends Serializable>
		extends ContentRepository<S, SID> {

	@Transactional
	@LockParticipant
	void setContent(S property, InputStream content);

	@Transactional
	@LockParticipant
	void unsetContent(S property);

	@Transactional
	InputStream getContent(S property);

}
