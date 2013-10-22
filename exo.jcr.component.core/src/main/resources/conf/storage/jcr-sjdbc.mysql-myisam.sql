/*$DELIMITER:/ */
CREATE TABLE JCR_SITEM(
	ID VARCHAR(96) NOT NULL,
	PARENT_ID VARCHAR(96) NOT NULL,
	NAME VARCHAR(512) CHARSET latin1 COLLATE latin1_general_cs NOT NULL,
	VERSION INTEGER NOT NULL,
	CONTAINER_NAME VARCHAR(96) NOT NULL,
	I_CLASS INTEGER NOT NULL,
	I_INDEX INTEGER NOT NULL,
	N_ORDER_NUM INTEGER,
	P_TYPE INTEGER, 
	P_MULTIVALUED BOOLEAN,	
	CONSTRAINT JCR_PK_SITEM PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_SITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_SITEM(ID)
) ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_SITEM_PARENT ON JCR_SITEM(CONTAINER_NAME, PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_SITEM_PARENT_NAME ON JCR_SITEM(I_CLASS, CONTAINER_NAME, PARENT_ID, NAME, I_INDEX, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_SITEM_PARENT_ID ON JCR_SITEM(I_CLASS, CONTAINER_NAME, PARENT_ID, ID, VERSION DESC)/
CREATE INDEX JCR_IDX_SITEM_N_ORDER_NUM ON JCR_SITEM(I_CLASS, CONTAINER_NAME, PARENT_ID, N_ORDER_NUM)/
CREATE TABLE JCR_SVALUE(
	ID SERIAL NOT NULL, 
	DATA LONGBLOB,
	ORDER_NUM INTEGER NOT NULL,
	PROPERTY_ID VARCHAR(96) NOT NULL,
	STORAGE_DESC VARCHAR(512),
	CONSTRAINT JCR_PK_SVALUE PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_SVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_SITEM(ID)
) ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_SVALUE_PROPERTY ON JCR_SVALUE(PROPERTY_ID, ORDER_NUM)/
CREATE INDEX JCR_IDX_SVALUE_STORAGE_DESC ON JCR_SVALUE(PROPERTY_ID, STORAGE_DESC)/
CREATE TABLE JCR_SREF(
  NODE_ID VARCHAR(96) NOT NULL, 
  PROPERTY_ID VARCHAR(96) NOT NULL,
  ORDER_NUM INTEGER NOT NULL,
  CONSTRAINT JCR_PK_SREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)
) ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_SREF_PROPERTY ON JCR_SREF(PROPERTY_ID, ORDER_NUM)/
CREATE TABLE JCR_SEQ (
  name VARCHAR(120) NOT NULL,
  nextVal INTEGER NOT NULL,
  CONSTRAINT JCR_PK_SEQ PRIMARY KEY (name))ENGINE=MyISAM/
CREATE FUNCTION JCR_NEXT_VAL(nameSeq VARCHAR(120)) RETURNS INTEGER
BEGIN
   DECLARE result INTEGER;
   UPDATE JCR_SEQ SET nextVal = LAST_INSERT_ID(nextVal + 1) WHERE name = nameSeq;
   SELECT nextVal INTO result from JCR_SEQ where name=nameSeq;
   RETURN result;
END/
