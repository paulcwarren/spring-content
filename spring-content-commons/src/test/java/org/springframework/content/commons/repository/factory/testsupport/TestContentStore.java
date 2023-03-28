package org.springframework.content.commons.repository.factory.testsupport;

import java.io.Serializable;

import org.springframework.content.commons.store.ContentStore;

public interface TestContentStore<S, SID extends Serializable> extends ContentStore<S, SID> {
}
