package internal.org.springframework.content.rest.support;

import java.util.UUID;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;

public interface TestEntity7Store extends ContentStore<TestEntity7, UUID>, Renderable<TestEntity7> {
}
