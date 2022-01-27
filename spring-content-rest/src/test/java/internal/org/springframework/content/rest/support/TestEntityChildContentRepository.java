package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;

public interface TestEntityChildContentRepository extends FilesystemContentStore<TestEntityChild, String>, Renderable<TestEntityChild> {
}
