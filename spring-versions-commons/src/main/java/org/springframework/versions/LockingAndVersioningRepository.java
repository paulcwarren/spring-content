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
     * @param <S> the type of entity
     * @param entity the entity to be locked
     * @return the locked entity
     */
    <S extends T> S lock(S entity);

    /**
     * Unlocks the entity and returns the updated entity (@Version and @LockOwner) artributes updated, otherwise
     * returns null
     *
     * @param <S> the type of entity
     * @param entity the entity to unlock
     * @return the unlocked entity
     */
    <S extends T> S unlock(S entity);

    /**
     * Overridden implementation of save that enforces locking semantics
     *
     * @param <S> the type of entity
     * @param entity the entity to save
     * @return the saved entity
     */
    <S extends T> S save(S entity);

    /**
     * Creates a new versionWithEntity of the entity.  This new versionWithEntity becomes the latest versionWithEntity.  The given entity becomes the
     * previous versionWithEntity and its lock is removed.
     *
     * After updating the existing entity's versioning attributes, persists and flushes those changes this method then
     * detaches the entity from the persistence context to effect a 'clone' that becomes the new versionWithEntity.
     *
     * @param <S> the type of entity
     * @param entity the entity to base the new versionWithEntity on
     * @param info the version info
     * @return the new versionWithEntity
     */
    <S extends T> S version(S entity, VersionInfo info);

    /**
     * Returns the latest versionWithEntity of all entities.  When using LockingAndVersioningRepository this method would
     * usually be preferred over CrudRepository's findAll that would find all versions of all entities.
     *
     * @param <S> the type of entity
     * @return list of latest versionWithEntity entities
     */
    @Query("select t from #{#entityName} t where t.successorId = null")
    <S extends T> List<S> findAllLatestVersion();

    /**
     * Returns a list of all versions of the given entity.
     *
     * @param <S> the type of entity
     * @param entity the entity to find versions for
     * @return list of entity versions
     */
    @Query("select t from #{#entityName} t where t.ancestralRootId = ?#{#entity.ancestralRootId}")
    <S extends T> List<S> findAllVersions(@Param("entity") S entity);

    /**
     * Deletes a given entity.
     *
     * @param <S> the type of entity
     * @param entity the entity to delete
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    <S extends T> void delete(S entity);
}
