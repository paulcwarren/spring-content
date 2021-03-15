package org.springframework.content.azure.store;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface AzureStorageContentStore<E,CID extends Serializable> extends ContentStore<E,CID> {
}
