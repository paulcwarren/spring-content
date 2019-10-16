package org.springframework.content.mongo.store;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface MongoContentStore<E, CID extends Serializable> extends ContentStore<E, CID> {
}
