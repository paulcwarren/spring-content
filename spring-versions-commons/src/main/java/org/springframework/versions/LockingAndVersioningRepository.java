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
     * Creates a new versionWithEntity of the entity.  This new versionWithEntity becomes the latest versionWithEntity.  The given entity becomes the
     * previous versionWithEntity and its lock is removed.
     *
     * After updating the existing entity's versioning attributes, persists and flushes those changes this method then
     * detaches the entity from the persistence context to effect a 'clone' that becomes the new versionWithEntity.
     *
     * @param entity the entity to base the new versionWithEntity on
     * @return the new versionWithEntity
     */
    <S extends T> S version(S entity, VersionInfo info);

    /**
     * Returns the latest versionWithEntity of all entities.  When using LockingAndVersioningRepository this method would
     * usually be preferred over CrudRepository's findAll that would find all versions of all entities.
     *
     * @return list of latest versionWithEntity entities
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
