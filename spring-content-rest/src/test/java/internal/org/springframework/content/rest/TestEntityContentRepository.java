package internal.org.springframework.content.rest;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@ContentStoreRestResource(path="/testEntities")
public interface TestEntityContentRepository extends ContentStore<TestEntity, Long> {
}


