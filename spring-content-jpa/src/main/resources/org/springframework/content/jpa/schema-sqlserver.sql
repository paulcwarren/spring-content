IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='BLOBS' AND xtype='U') CREATE TABLE BLOBS ( id int IDENTITY(1,1), content varBinary(MAX) )
