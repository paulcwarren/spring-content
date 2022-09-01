package internal.org.springframework.content.rest.support;

import java.util.UUID;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(/*linkRel = "foo"*/)
public interface TestEntity10Store extends FilesystemContentStore<TestEntity10, UUID>, Renderable<TestEntity10> {
}
