package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

public interface TestEntity3Repository extends JpaRepository<TestEntity3, Long> {

    TestEntity3 save(TestEntity3 entity);
}
