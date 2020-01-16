package org.springframework.content.fs.store;

import java.io.Serializable;

import org.springframework.content.commons.repository.AssociativeStore;

public interface FilesystemAssociativeStore<I, CID extends Serializable> extends AssociativeStore<I, CID> {
}
