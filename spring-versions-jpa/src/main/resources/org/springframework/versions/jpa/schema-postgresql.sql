CREATE TABLE IF NOT EXISTS locks(
    entity_id VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_owner VARCHAR(64)
);
CREATE UNIQUE INDEX IF NOT EXISTS locks_pk ON locks(entity_id, lock_owner);
