package org.springframework.versions;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LockingAndVersioningRepository<T, ID extends Serializable> {

    /**
     * Locks the entity and returns the updated entity (@Version and @LockOwner) attributes updated, otherwise
     * returns null.
     *
     * @param <S> the type of entity
     * @param entity the entity to be locked
     * @return the locked entity
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S lock(S entity);

    /**
     * Unlocks the entity and returns the updated entity (@Version and @LockOwner) attributes updated, otherwise
     * returns null
     *
     * @param <S> the type of entity
     * @param entity the entity to unlock
     * @return the unlocked entity
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S unlock(S entity);

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
     * Creates and returns a new version of the entity as a private working copy.  The given entity remains the latest
     * version.
     *
     * This method requires the entity class to have a copy constructor used for cloning the new version instance.
     *
     * @param <S> the type of entity
     * @param entity the entity to base the new versionWithEntity on
     * @return the private working copy
     * @throws LockingAndVersioningException if entity is not the latest
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S createPrivateWorkingCopy(S entity);

    /**
     * Creates and returns a new version of the entity.  This new version becomes the latest version in the version
     * list.
     *
     * This method requires the entity class to have a copy constructor used for cloning the new version instance.
     *
     * @param <S> the type of entity
     * @param entity the entity to base the new versionWithEntity on
     * @param info the version info
     * @return the new versionWithEntity
     * @throws LockingAndVersioningException if entity is not the latest
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S version(S entity, VersionInfo info);

    /**
     * Returns the latest version of all entities.  When extending LockingAndVersioningRepository this
     * method would usually be preferred over CrudRepository's findAll that would find all versions
     * of all entities.
     *
     * @param <S> the type of entity
     * @return list of latest versionWithEntity entities
     */
    @Query("select t from #{#entityName} t where t.successorId = null")
    <S extends T> List<S> findAllVersionsLatest();

    /**
     * Returns a list of all versions for the given entity.
     *
     * @param <S> the type of entity
     * @param entity the entity to find versions for
     * @return list of entity versions
     */
    @Query("select t from #{#entityName} t where t.ancestralRootId = ?#{#entity.ancestralRootId}")
    <S extends T> List<S> findAllVersions(@Param("entity") S entity);

    /**
     * Deletes a given entity version.  The entity must be the head of the version list.
     *
     * If the entity is locked the lock will be carried over to the previous version when
     * it becomes the new head.
     *
     * @param <S> the type of entity
     * @param entity the entity to delete
     * @throws LockingAndVersioningException if entity is not the latest
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> void delete(S entity);
}
