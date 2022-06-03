package internal.org.springframework.content.rest.support;

import java.io.InputStream;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.RestResource;

public interface TestEntity5Store extends FilesystemContentStore<TestEntity5, Long> {

    @RestResource(paths={"rendition"}, exported=false)
    @Override
    InputStream getContent(TestEntity5 entity, PropertyPath propertyPath);
}
