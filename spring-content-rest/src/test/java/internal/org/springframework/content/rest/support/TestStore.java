package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FilesystemStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "teststore")
public interface TestStore extends FilesystemStore<String> {
}
