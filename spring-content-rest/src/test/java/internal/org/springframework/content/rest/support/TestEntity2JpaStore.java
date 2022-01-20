package internal.org.springframework.content.rest.support;

import java.util.UUID;

import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "files")
public interface TestEntity2JpaStore extends JpaContentStore<TestEntity2, UUID> {
}
