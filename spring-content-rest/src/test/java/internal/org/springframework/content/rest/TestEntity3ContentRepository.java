package internal.org.springframework.content.rest;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@ContentStoreRestResource(path="testEntities")
public interface TestEntity3ContentRepository extends ContentStore<TestEntity3, Long> {
}


