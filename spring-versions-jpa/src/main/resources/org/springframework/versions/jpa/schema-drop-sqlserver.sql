DROP INDEX IF EXISTS locks_pk ON locks;
IF EXISTS (SELECT * FROM sysobjects WHERE name='locks' AND xtype='U') DROP TABLE locks;
