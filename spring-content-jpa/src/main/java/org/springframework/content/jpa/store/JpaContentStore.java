package org.springframework.content.jpa.store;

import java.io.Serializable;

import org.springframework.content.commons.store.ContentStore;

public interface JpaContentStore<T, CID extends Serializable> extends ContentStore<T, CID> {
}
