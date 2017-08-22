package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.repository.Store;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@ContentStoreRestResource(path="teststore")
public interface TestStore extends Store<String> {
}


