CREATE TABLE locks(entity_id VARCHAR(64) NOT NULL PRIMARY KEY, lock_owner VARCHAR(64));
CREATE UNIQUE INDEX locks_pk ON locks(entity_id, lock_owner);

