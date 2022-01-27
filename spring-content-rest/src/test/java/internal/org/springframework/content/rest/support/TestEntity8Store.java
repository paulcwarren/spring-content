package internal.org.springframework.content.rest.support;

import java.util.UUID;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;

public interface TestEntity8Store extends FilesystemContentStore<TestEntity8, UUID>, Renderable<TestEntity8> {
}
