/*$DELIMITER:/ */
CREATE TABLE JCR_MITEM(
	ID VARCHAR(96) NOT NULL,
	PARENT_ID VARCHAR(96) NOT NULL,
	NAME VARCHAR(512) CHARSET latin1 COLLATE latin1_general_cs NOT NULL,
	VERSION INTEGER NOT NULL, 
	I_CLASS INTEGER NOT NULL,
	I_INDEX INTEGER NOT NULL,
	N_ORDER_NUM INTEGER,
	P_TYPE INTEGER, 
	P_MULTIVALUED BOOLEAN,	
	CONSTRAINT JCR_PK_MITEM PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_MITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM(ID)
)  ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT ON JCR_MITEM(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME ON JCR_MITEM(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID ON JCR_MITEM(I_CLASS, PARENT_ID, ID, VERSION DESC)/
CREATE INDEX JCR_IDX_MITEM_N_ORDER_NUM ON JCR_MITEM(I_CLASS, PARENT_ID, N_ORDER_NUM)/
CREATE TABLE JCR_MVALUE(
	ID SERIAL NOT NULL, 
	DATA LONGBLOB, 
	ORDER_NUM INTEGER NOT NULL, 
	PROPERTY_ID VARCHAR(96) NOT NULL,
	STORAGE_DESC VARCHAR(512),
	CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MITEM(ID)
)  ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_MVALUE_PROPERTY ON JCR_MVALUE(PROPERTY_ID, ORDER_NUM)/
CREATE INDEX JCR_IDX_MVALUE_STORAGE_DESC ON JCR_MVALUE(PROPERTY_ID, STORAGE_DESC)/
CREATE TABLE JCR_MREF(
  NODE_ID VARCHAR(96) NOT NULL, 
  PROPERTY_ID VARCHAR(96) NOT NULL,
  ORDER_NUM INTEGER NOT NULL,
  CONSTRAINT JCR_PK_MREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)
)  ENGINE=MyISAM/
CREATE UNIQUE INDEX JCR_IDX_MREF_PROPERTY ON JCR_MREF(PROPERTY_ID, ORDER_NUM)/
CREATE TABLE JCR_MITEM_SEQ (
  name VARCHAR(20) NOT NULL,
  nextVal INTEGER NOT NULL,
  CONSTRAINT JCR_PK_MITEM_SEQ PRIMARY KEY (name))ENGINE=MyISAM/
CREATE FUNCTION JCR_MITEM_NEXT_VAL (nameSeq VARCHAR(120),newVal INTEGER, increment INTEGER ) RETURNS INTEGER
BEGIN
   DECLARE result INTEGER;
   IF (increment = 1)
   THEN
     UPDATE JCR_MITEM_SEQ SET nextVal = LAST_INSERT_ID(nextVal + 1) WHERE name = nameSeq;
   ELSE
      SELECT nextVal INTO result from JCR_MITEM_SEQ where name=nameSeq;
      IF (result < newVal)
      THEN
        UPDATE JCR_MITEM_SEQ SET nextVal = newVal WHERE name = nameSeq;
      END IF;
   END IF;
   SELECT nextVal INTO result from JCR_MITEM_SEQ where name=nameSeq;
   RETURN result;
END/
