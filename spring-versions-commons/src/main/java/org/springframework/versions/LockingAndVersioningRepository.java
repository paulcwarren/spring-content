package org.springframework.versions;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;

public interface LockingAndVersioningRepository<T, ID extends Serializable> {

    /**
     * Locks the entity and returns the updated entity (@Version and @LockOwner) artributes updated, otherwise
     * returns null
     *
     * @param entity
     * @return
     */
    <S extends T> S lock(S entity);

    /**
     * Unlocks the entity and returns the updated entity (@Version and @LockOwner) artributes updated, otherwise
     * returns null
     *
     * @param entity
     * @return
     */
    <S extends T> S unlock(S entity);

    /**
     * Overridden implementation of save that enforces locking semantics
     *
     * @param entity
     * @param <S>
     * @return
     */
    <S extends T> S save(S entity);

    /**
     * Creates a new version of the entity.  This new version becomes the latest version.  The given entity becomes the
     * previous version and its lock is removed.
     *
     * @param entity the entity to base the new version on
     * @return the new version
     */
    <S extends T> S version(S entity);

    /**
     * Returns the latest version of all entities.  When using LockingAndVersioningRepository this method would
     * usually be preferred over CrudRepository's findAll that would find all versions of all entities.
     *
     * @return list of latest version entities
     */
    @Query("select t from #{#entityName} t where t.latest = true")
    <S extends T> List<S> findAllLatestVersion();

    /**
     * Returns a list of all versions of the given entity.
     *
     * @param entity
     * @return list of entity versions
     */
    @Query("select t from #{#entityName} t where t.ancestralRootId = ?#{#entity.ancestralRootId}")
    <S extends T> List<S> findAllVersions(@Param("entity") S entity);
}
