package internal.org.springframework.content.rest.it;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.jpa.store.JpaContentStore;

import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.support.TestEntityChild;

@ContentStoreRestResource(path = "files")
public interface TestEntityChildContentRepository
		extends JpaContentStore<TestEntityChild, String>, Renderable<TestEntityChild> {
}
