package internal.org.springframework.content.rest.support;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource
public interface TestEntity5Store extends FilesystemContentStore<TestEntity5, UUID> {

    @RestResource(paths={"rendition"}, exported=false)
    @Override
    InputStream getContent(TestEntity5 entity, PropertyPath propertyPath);
}
