package org.springframework.content.gcs.store;

import java.io.Serializable;

import org.springframework.content.commons.store.ContentStore;

public interface GCPStorageContentStore<E,CID extends Serializable> extends ContentStore<E,CID> {
}
