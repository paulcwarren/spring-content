CREATE TABLE locks
(
    entity_id VARCHAR(64) NOT NULL,
    lock_owner VARCHAR(64),
    PRIMARY KEY(entity_id, lock_owner)
);
