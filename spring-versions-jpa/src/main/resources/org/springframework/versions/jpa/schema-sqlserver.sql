IF NOT EXISTS (
    SELECT * FROM sysobjects
    WHERE name='locks' AND xtype='U')
CREATE TABLE locks(entity_id VARCHAR(64) NOT NULL PRIMARY KEY, lock_owner VARCHAR(64));
IF NOT EXISTS (
    SELECT 'foo' FROM sys.indexes
    WHERE object_id = OBJECT_ID('locks')
    AND name='locks_pk')
CREATE UNIQUE INDEX locks_pk ON locks(entity_id, lock_owner);

