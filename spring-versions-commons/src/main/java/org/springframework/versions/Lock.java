package org.springframework.versions;

import java.io.Serializable;

public class Lock {

    private Serializable entityId;
    private String lockOwner;

    public Lock(Serializable id, String name) {
        this.entityId = id;
        this.lockOwner = name;
    }

    public Serializable getEntityId() {
        return entityId;
    }

    public void setEntityId(Serializable entityId) {
        this.entityId = entityId;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }
}
