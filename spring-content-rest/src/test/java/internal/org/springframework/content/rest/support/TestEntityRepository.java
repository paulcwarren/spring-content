package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path="testEntities")
public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
}


