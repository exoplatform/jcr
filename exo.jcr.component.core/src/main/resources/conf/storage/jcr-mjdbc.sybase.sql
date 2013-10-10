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
CREATE INDEX JCR_IDX_MITEM_N_ORDER_NUM ON JCR_MITEM(I_CLASS, PARENT_ID, N_ORDER_NUM);
CREATE TABLE JCR_MVALUE(
    ID BIGINT IDENTITY NOT NULL, 
	DATA IMAGE NULL,
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
CREATE SEQUENCE JCR_N_ORDER_NUM  INCREMENT BY 1  NO MAXVALUE   NO CYCLE;