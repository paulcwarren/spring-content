CREATE TABLE IF NOT EXISTS locks(
    entity_id VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_owner VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SELECT IF (
    EXISTS (
        SELECT DISTINCT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE()
        AND table_name = 'locks' AND index_name like 'locks_pk'
    )
    ,'SELECT ''index index_1 exists'' _______;'
    ,'CREATE UNIQUE INDEX locks_pk ON locks(entity_id,lock_owner)') INTO @a;
PREPARE stmt1 FROM @a;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;
