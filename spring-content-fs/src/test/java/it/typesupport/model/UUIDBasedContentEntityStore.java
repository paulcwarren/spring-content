package it.typesupport.model;

import org.springframework.content.commons.repository.ContentStore;

import java.util.UUID;

public interface UUIDBasedContentEntityStore extends ContentStore<UUIDBasedContentEntity, UUID> {

}
