package internal.org.springframework.content.rest;

import org.springframework.content.commons.repository.ContentStore;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@ContentStoreRestResource(path="testEntity4s")
public interface TestEntity4ContentRepository extends ContentStore<TestEntity4, Long> {
}


