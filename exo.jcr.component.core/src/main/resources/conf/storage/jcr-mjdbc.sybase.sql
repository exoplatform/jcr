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
  VERSION VARCHAR(96) NOT NULL,
	CONSTRAINT JCR_PK_MCONTAINER PRIMARY KEY(VERSION)
);
CREATE TABLE JCR_MITEM(
	ID VARCHAR(96) NOT NULL,
	PARENT_ID VARCHAR(96) NOT NULL,
	NAME VARCHAR(512) NOT NULL,
	VERSION INT NOT NULL, 
	I_CLASS INT NOT NULL,
	I_INDEX INT NOT NULL,
	N_ORDER_NUM INT NULL,
	P_TYPE INT NULL, 
	P_MULTIVALUED INT NULL,	
	CONSTRAINT JCR_PK_MITEM PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_MITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM(ID)
);
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT ON JCR_MITEM(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION DESC);
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME ON JCR_MITEM(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION DESC);
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID ON JCR_MITEM(I_CLASS, PARENT_ID, ID, VERSION DESC);
CREATE TABLE JCR_MVALUE(
    ID BIGINT IDENTITY NOT NULL, 
	DATA VARBINARY(255) NULL,
    ORDER_NUM INT NOT NULL,
    PROPERTY_ID VARCHAR(96) NOT NULL,
	STORAGE_DESC VARCHAR(512) NULL,
	CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),
    CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MITEM(ID)
);
CREATE UNIQUE INDEX JCR_IDX_MVALUE_PROPERTY ON JCR_MVALUE(PROPERTY_ID, ORDER_NUM);
CREATE TABLE JCR_MREF(
  NODE_ID VARCHAR(96) NOT NULL,
  PROPERTY_ID VARCHAR(96) NOT NULL,
  ORDER_NUM INT NOT NULL,
  CONSTRAINT JCR_PK_MREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)
);
CREATE UNIQUE INDEX JCR_IDX_MREF_PROPERTY ON JCR_MREF(PROPERTY_ID, ORDER_NUM);