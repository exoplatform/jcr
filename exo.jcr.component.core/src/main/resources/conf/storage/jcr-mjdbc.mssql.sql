/*$DELIMITER:/ */
CREATE TABLE JCR_MITEM(
	ID VARCHAR(96) NOT NULL,
	PARENT_ID VARCHAR(96) NOT NULL,
	NAME VARCHAR(512) NOT NULL,
	VERSION INTEGER NOT NULL, 
	I_CLASS INTEGER NOT NULL,
	I_INDEX INTEGER NOT NULL,
	N_ORDER_NUM INTEGER,
	P_TYPE INTEGER, 
	P_MULTIVALUED INTEGER,	
	CONSTRAINT JCR_PK_MITEM PRIMARY KEY(ID),
	CONSTRAINT JCR_FK_MITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM(ID)
)/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT ON JCR_MITEM(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME ON JCR_MITEM(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION DESC)/
CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID ON JCR_MITEM(I_CLASS, PARENT_ID, ID, VERSION DESC)/
CREATE INDEX JCR_IDX_MITEM_N_ORDER_NUM ON JCR_MITEM(I_CLASS, PARENT_ID, N_ORDER_NUM)/
CREATE TABLE JCR_MVALUE(
    ID BIGINT IDENTITY(1,1) NOT NULL,
    DATA VARBINARY(MAX),
    ORDER_NUM INTEGER NOT NULL,
    PROPERTY_ID VARCHAR(96) NOT NULL,
	STORAGE_DESC VARCHAR(512),
	CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),
    CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MITEM(ID)
)/
CREATE UNIQUE INDEX JCR_IDX_MVALUE_PROPERTY ON JCR_MVALUE(PROPERTY_ID, ORDER_NUM)/
CREATE TABLE JCR_MREF(
  	NODE_ID VARCHAR(96) NOT NULL,
  	PROPERTY_ID VARCHAR(96) NOT NULL,
  	ORDER_NUM INTEGER NOT NULL,
  	CONSTRAINT JCR_PK_MREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)
)/
CREATE UNIQUE INDEX JCR_IDX_MREF_PROPERTY ON JCR_MREF(PROPERTY_ID, ORDER_NUM)/
CREATE TABLE JCR_SEQ (
    name VARCHAR(20) NOT NULL,
    nextVal INTEGER NOT NULL,
    CONSTRAINT JCR_PK_SEQ PRIMARY KEY (name)
)/
CREATE PROCEDURE JCR_NEXT_VAL
    @name VARCHAR(20)
AS
    BEGIN
        SET NOCOUNT ON
        DECLARE @value INTEGER
        BEGIN TRANSACTION
            UPDATE JCR_SEQ
            SET @value=nextVal=nextVal + 1
            WHERE name = @name;
        COMMIT TRANSACTION
        SELECT @value AS nextval
        return
    END/