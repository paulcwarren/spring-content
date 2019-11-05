package org.springframework.content.commons.repository;

import java.io.InputStream;

import org.springframework.versions.LockParticipant;

public interface ContentStore<S, SID> extends ContentRepository<S, SID> {

	@LockParticipant
	S setContent(S property, InputStream content);

	@LockParticipant
	S unsetContent(S property);

	InputStream getContent(S property);

}
