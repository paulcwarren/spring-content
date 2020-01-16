package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.web.bind.annotation.CrossOrigin;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@CrossOrigin(origins = "http://www.someurl.com")
@ContentStoreRestResource(path = "testEntitiesContent")
public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, Long>, Renderable<TestEntity> {
}
