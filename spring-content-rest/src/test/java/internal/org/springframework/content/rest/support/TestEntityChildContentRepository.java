package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@ContentStoreRestResource(path = "files")
public interface TestEntityChildContentRepository extends FilesystemContentStore<TestEntityChild, String>, Renderable<TestEntityChild> {
}
