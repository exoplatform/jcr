delete from JCR_SVALUE where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?);
delete from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?);
delete from JCR_SITEM where CONTAINER_NAME=?;
