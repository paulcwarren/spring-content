package internal.org.springframework.content.rest.support;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://www.someurl.com")
@ContentStoreRestResource(path = "testEntitiesContent")
public interface TestEntityContentRepository
		extends ContentStore<TestEntity, Long>, Renderable<TestEntity> {
}
