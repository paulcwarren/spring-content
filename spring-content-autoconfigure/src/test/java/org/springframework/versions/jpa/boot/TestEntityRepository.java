package org.springframework.versions.jpa.boot;

import org.springframework.data.repository.CrudRepository;
import org.springframework.support.TestEntityVersioned;
import org.springframework.versions.LockingAndVersioningRepository;

public interface TestEntityRepository extends CrudRepository<TestEntityVersioned, Long>, LockingAndVersioningRepository<TestEntityVersioned, Long> {}
