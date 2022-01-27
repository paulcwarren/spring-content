package internal.org.springframework.content.rest.support;

import java.util.UUID;

import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "files")
public interface TestEntity2Store extends FilesystemContentStore<TestEntity2, UUID> {
}
