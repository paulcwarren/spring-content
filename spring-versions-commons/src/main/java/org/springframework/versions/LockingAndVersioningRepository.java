package org.springframework.versions;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.rest.core.annotation.RestResource;

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
     * This method requires the entity class to have a copy constructor that will be used to clone the entity in order
     * to create the new working copy.
     *
     * @param <S> the type of entity
     * @param entity the entity to base the new versionWithEntity on
     * @return the private working copy
     * @throws LockingAndVersioningException if entity is not the latest
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    <S extends T> S workingCopy(S entity);

    /**
     * Creates and returns a new version of the entity.  This new version becomes the latest version in the version
     * list.
     *
     * If the supplied entity is a private working copy, it will be promoted from a working copy to the new version.
     *
     * If the supplied entity is not a private working copy, the entity will be cloned in order to create the new
     * version.  This requires the entity class to have a copy constructor that is used for the cloning process.
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
     * @deprecated
     */
    @RestResource(exported=false)
    @Deprecated
    <S extends T> List<S> findAllVersionsLatest();

    /**
     * Returns the latest version of all entities.  When extending LockingAndVersioningRepository this
     * method would usually be preferred over CrudRepository's findAll that would find all versions
     * of all entities.
     *
     * @param <S> the type of entity
     * @param entityClass the type of the entity to find
     * @return list of latest versionWithEntity entities
     */
    <S extends T> List<S> findAllVersionsLatest(Class<S> entityClass);

    /**
     * Returns a list of all versions for the given entity.
     *
     * @param <S> the type of entity
     * @param entity the entity to find versions for
     * @return list of entity versions
     */
    <S extends T> List<S> findAllVersions(S entity);

    /**
     * Returns a sorted list of all versions for the given entity
     *
     * @param <S> the type of entity
     * @param entity the entity to find versions for
     * @param sort the sort to apply
     * @return list of entity versions
     */
    <S extends T> List<S> findAllVersions(S entity, Sort sort);

    /**
     * Deletes a given entity version.  The entity must be the head of the version list.
     *
     * If the entity is locked the lock will be carried over to the previous version when
     * it becomes the new head.
     *
     * @param entity the entity to delete
     * @throws LockingAndVersioningException if entity is not the latest
     * @throws LockOwnerException if the current principal is not the lock owner
     * @throws SecurityException if no authentication exists
     */
    void delete(T entity);

    /**
     * Deletes all versions of the given entity.
     *
     * @param entity the entity to delete
     */
    void deleteAllVersions(T entity);

    /**
     * Returns whether the given entity is a private working copy, or not
     *
     * @param <S> the type of entity
     * @param entity the entity to delete
     * @return true if entity is a private working copy, otherwise false
     */
    <S extends T> boolean isPrivateWorkingCopy(S entity);

    /**
     * Returns the working copy for the given entity if it exists.
     *
     * @param <S> the type of entity
     * @param entity the entity whose working copy is required
     * @return the working copy if it exists, or null
     */
    <S extends T> S findWorkingCopy(S entity);
}
