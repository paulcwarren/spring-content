package org.springframework.content.commons.repository;

import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;
import org.springframework.versions.LockParticipant;

public interface ContentStore<S, SID extends Serializable> extends AssociativeStore<S, SID>, ContentRepository<S, SID> {

	@LockParticipant
	S setContent(S property, InputStream content);

    @LockParticipant
    S setContent(S property, PropertyPath propertyPath, InputStream contentm);

	@LockParticipant
	S setContent(S property, Resource resourceContent);

    @LockParticipant
    S setContent(S property, PropertyPath propertyPath, Resource resourceContent);

	@LockParticipant
	S unsetContent(S property);

    @LockParticipant
    S unsetContent(S property, PropertyPath propertyPath);

	InputStream getContent(S property);

    InputStream getContent(S property, PropertyPath propertyPath);
}
