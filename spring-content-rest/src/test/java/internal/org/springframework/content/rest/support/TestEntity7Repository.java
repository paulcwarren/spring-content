package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;

public interface TestEntity7Repository extends CrudRepository<TestEntity7, Long> {

    TestEntity7 findByName(String name);
}
