/* --Before database will be created we have to create 'disk' with enough amount of free space
use master
go 

disk init  name  = 'EXOJCR',
physname  = '/opt/Sybase/ASELE/data/exojcr',
size  = '400M',
cntrltype  = 0
go 

sp_deviceattr 'EXOJCR', dsync, true
go

-- Creating database 'portal', 'jcr', 'jcr2'
use master
go

create database portal on EXOJCR
go
create database jcr on EXOJCR
go
create database jcr2 on EXOJCR
go

-- Enable indexes in database turning on 'select into' option
use master
go
sp_dboption portal, 'select into', true
go
sp_dboption jcr, 'select into', true
go
sp_dboption jcr2, 'select into', true
go

-- NOTE: User creating tables we must have a DBO rights for the target database
-- NOTE: Index maximum length is 600 bytes
-- NOTE: VARBINARY storage size is the actual size of the data values entered, not the column length.
-- NOTE: set number of user connection according your pool configuration: sp_configure "number of user connection", 200
*/

CREATE TABLE JCR_MCONTAINER(
  VERSION VARCHAR(255) NOT NULL PRIMARY KEY
  );
CREATE TABLE JCR_MITEM(
	ID VARCHAR(255) NOT NULL PRIMARY KEY, 
	VERSION INT NOT NULL, 
	PATH VARCHAR(1024) NOT NULL
	);
CREATE INDEX JCR_IDX_MITEM_IDPATH ON JCR_MITEM(ID, SUBSTRING(PATH, 1, 345));
CREATE TABLE JCR_MNODE(
	ID VARCHAR(255) NOT NULL PRIMARY KEY, 
	ORDER_NUM INTEGER NULL, 
	PARENT_ID VARCHAR(255) NULL, 
	CONSTRAINT JCR_FK_MNODE_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MNODE(ID), 
	CONSTRAINT JCR_FK_MNODE_ITEM FOREIGN KEY(ID) REFERENCES JCR_MITEM(ID)
	);
CREATE INDEX JCR_IDX_MNODE_PARENT ON JCR_MNODE(ID, PARENT_ID, ORDER_NUM);
CREATE TABLE JCR_MPROPERTY(
	ID VARCHAR(255) NOT NULL PRIMARY KEY,
	TYPE INT NOT NULL, 
	PARENT_ID VARCHAR(255) NOT NULL, 
	MULTIVALUED INT NOT NULL, 
	CONSTRAINT JCR_FK_MPROPERTY_NODE FOREIGN KEY(PARENT_ID) REFERENCES JCR_MNODE(ID), 
	CONSTRAINT JCR_FK_MPROPERTY_ITEM FOREIGN KEY(ID) REFERENCES JCR_MITEM(ID) 
	);
CREATE INDEX JCR_IDX_MPROPERTY_PARENT ON JCR_MPROPERTY(ID, PARENT_ID);
CREATE INDEX JCR_IDX_MPROPERTY_TYPE ON JCR_MPROPERTY(ID, TYPE);
CREATE TABLE JCR_MVALUE(
	ID BIGINT IDENTITY NOT NULL PRIMARY KEY, 
	DATA VARBINARY(255) NOT NULL, 
	ORDER_NUM INT NULL, 
	PROPERTY_ID VARCHAR(255) NOT NULL, 
	CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MPROPERTY(ID)
	);
CREATE INDEX JCR_IDX_MVALUE_PROPERTY ON JCR_MVALUE(PROPERTY_ID, ORDER_NUM);	
CREATE INDEX JCR_IDX_MVALUE_DATA ON JCR_MVALUE(PROPERTY_ID, DATA);
CREATE TABLE JCR_SREF(
  NODE_ID VARCHAR(255) NOT NULL, 
  PROPERTY_ID VARCHAR(255) NOT NULL,
  ORDER_NUM INTEGER NOT NULL,
  CONSTRAINT JCR_PK_SREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)
);
CREATE UNIQUE INDEX JCR_IDX_SREF_PROPERTY ON JCR_SREF(PROPERTY_ID, ORDER_NUM);