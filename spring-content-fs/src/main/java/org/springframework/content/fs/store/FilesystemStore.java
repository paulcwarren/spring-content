package org.springframework.content.fs.store;

import java.io.Serializable;

import org.springframework.content.commons.repository.Store;

public interface FilesystemStore<CID extends Serializable> extends Store<CID> {
}
