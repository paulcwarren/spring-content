package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(/*linkRel = "foo"*/)
public interface TestEntity11Store extends FilesystemContentStore<TestEntity11, UUID> {
}
