package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://www.someurl.com")
@RepositoryRestResource(path = "testEntities")
public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
}
