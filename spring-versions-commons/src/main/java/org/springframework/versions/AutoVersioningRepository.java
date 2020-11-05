package org.springframework.versions;

import java.io.Serializable;
import java.util.List;

public interface AutoVersioningRepository<T, ID extends Serializable, V> {

    /**
     * Overridden implementation of save that enforces locking semantics
     *
     * @param <S> the type of entity
     * @param entity the entity to save
     * @return the saved entity
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S save(S entity);

    /**
     * Returns a list of all versions for the given entity.
     *
     * @param <S> the type of entity
     * @param entity the entity to find versions for
     * @return list of entity versions
     */
    <S extends T> List<V> findAllVersions(S entity);
}
