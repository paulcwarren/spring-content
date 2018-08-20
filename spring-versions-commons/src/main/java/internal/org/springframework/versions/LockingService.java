package internal.org.springframework.versions;

import java.security.Principal;

public interface LockingService {

    /**
     * Locks the given entity for the given principal.
     *
     * If no lock exists for the given entity then the lock will be obtained.
     *
     * @param entityId the entity to lock
     * @return  true if lock succeeds, otherwise false
     */
    boolean lock(Object entityId, Principal principal);

    /**
     * Unlocks the given entity for the current principal.
     *
     * If the current principal holds the lock it will be released.
     *
     * @param entityId the entity to unlock
     * @return  true if unlock succeeds, otherwise false
     */
    boolean unlock(Object entityId, Principal principal);

    /**
     * Return true if the given principal holds the lock for the given entity.
     *
     * @param entityId  the entity with the lock
     * @param principal the principal
     * @return true if the given principal holds the lock for the given entity
     */
    boolean isLockOwner(Object entityId, Principal principal);

    /**
     * Returns the lock owner, or null if not locked
     *
     * @param entityId  the entity with the lock
     * @returnt lock owner
     */
    Principal lockOwner(Object entityId);
}
