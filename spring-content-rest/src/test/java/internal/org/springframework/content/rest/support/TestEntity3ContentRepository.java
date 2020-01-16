package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;

public interface TestEntity3ContentRepository extends FilesystemContentStore<TestEntity3, Long>, Renderable<TestEntity3> {
}
