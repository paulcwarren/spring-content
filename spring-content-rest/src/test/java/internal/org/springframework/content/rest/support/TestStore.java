package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.repository.Store;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "teststore")
public interface TestStore extends Store<String> {
}
