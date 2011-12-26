/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Single database connection implementation.
 * 
 * Created by The eXo Platform SAS 27.04.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: SingleDbJDBCConnection.java 20950 2008-10-06 14:23:07Z
 *          pnedonosko $
 */
public class SingleDbJDBCConnection extends JDBCStorageConnection
{

   /**
    * Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection JDBC connection, shoudl be opened before
    * @param readOnly, boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerName Workspace Storage Container name (see configuration)
    * @param valueStorageProvider External Value Storages provider
    * @param maxBufferSize Maximum buffer size (see configuration)
    * @param swapDirectory Swap directory (see configuration)
    * @param swapCleaner Swap cleaner (internal FileCleaner).
    * @throws SQLException in case of database error
    * @see org.exoplatform.services.jcr.impl.util.io.FileCleaner
    */
   public SingleDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {

      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getInternalId(final String identifier)
   {
      return containerName + identifier;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getIdentifier(final String internalId)
   {

      if (internalId == null) // possible for root parent
         return null;

      return internalId.substring(containerName.length());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {

      JCR_PK_ITEM = "JCR_PK_SITEM";
      JCR_FK_ITEM_PARENT = "JCR_FK_SITEM_PARENT";
      JCR_IDX_ITEM_PARENT = "JCR_IDX_SITEM_PARENT";
      JCR_IDX_ITEM_PARENT_NAME = "JCR_IDX_SITEM_PARENT_NAME";
      JCR_IDX_ITEM_PARENT_ID = "JCR_IDX_SITEM_PARENT_ID";
      JCR_PK_VALUE = "JCR_PK_SVALUE";
      JCR_FK_VALUE_PROPERTY = "JCR_FK_SVALUE_PROPERTY";
      JCR_IDX_VALUE_PROPERTY = "JCR_IDX_SVALUE_PROPERTY";
      JCR_PK_REF = "JCR_PK_SREF";
      JCR_IDX_REF_PROPERTY = "JCR_IDX_SREF_PROPERTY";

      FIND_ITEM_BY_ID = "select * from JCR_SITEM where ID=?";

      FIND_ITEM_BY_NAME =
         "select * from JCR_SITEM"
            + " where CONTAINER_NAME=? and PARENT_ID=? and NAME=? and I_INDEX=? order by I_CLASS, VERSION DESC";

      FIND_PROPERTY_BY_NAME =
         "select V.DATA"
            + " from JCR_SITEM I, JCR_SVALUE V"
            + " where I.I_CLASS=2 and I.CONTAINER_NAME=? and I.PARENT_ID=? and I.NAME=? and"
            + " I.ID=V.PROPERTY_ID order by V.ORDER_NUM";

      FIND_REFERENCES =
         "select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME" + " from JCR_SREF R, JCR_SITEM P"
            + " where R.NODE_ID=? and P.CONTAINER_NAME=? and P.ID=R.PROPERTY_ID and P.I_CLASS=2";

      FIND_VALUES_BY_PROPERTYID =
         "select PROPERTY_ID, ORDER_NUM, DATA, STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=? order by ORDER_NUM";

      FIND_VALUES_VSTORAGE_DESC_BY_PROPERTYID = "select distinct STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=?";

      FIND_VALUE_BY_PROPERTYID_OREDERNUMB =
         "select DATA, STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=? and ORDER_NUM=?";

      FIND_NODES_BY_PARENTID =
         "select * from JCR_SITEM" + " where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?" + " order by N_ORDER_NUM";

      FIND_LAST_ORDER_NUMBER_BY_PARENTID =
         "select count(*), max(N_ORDER_NUM) from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

      FIND_NODES_COUNT_BY_PARENTID =
         "select count(ID) from JCR_SITEM" + " where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

      FIND_PROPERTIES_BY_PARENTID =
         "select * from JCR_SITEM" + " where I_CLASS=2 and CONTAINER_NAME=? and PARENT_ID=?" + " order by ID";

      INSERT_NODE =
         "insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM) VALUES(?,?,?,?,?,"
            + I_CLASS_NODE + ",?,?)";
      INSERT_PROPERTY =
         "insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) "
            + "VALUES(?,?,?,?,?," + I_CLASS_PROPERTY + ",?,?,?)";

      INSERT_VALUE = "insert into JCR_SVALUE(DATA, ORDER_NUM, PROPERTY_ID, STORAGE_DESC) VALUES(?,?,?,?)";
      INSERT_REF = "insert into JCR_SREF(NODE_ID, PROPERTY_ID, ORDER_NUM) VALUES(?,?,?)";

      RENAME_NODE = "update JCR_SITEM set PARENT_ID=?, NAME=?, VERSION=?, I_INDEX=?, N_ORDER_NUM=? where ID=?";

      UPDATE_NODE = "update JCR_SITEM set VERSION=?, I_INDEX=?, N_ORDER_NUM=? where ID=?";
      UPDATE_PROPERTY = "update JCR_SITEM set VERSION=?, P_TYPE=? where ID=?";
      //UPDATE_VALUE = "update JCR_SVALUE set DATA=?, STORAGE_DESC=? where PROPERTY_ID=?, ORDER_NUM=?";

      DELETE_ITEM = "delete from JCR_SITEM where ID=?";
      DELETE_VALUE = "delete from JCR_SVALUE where PROPERTY_ID=?";
      DELETE_REF = "delete from JCR_SREF where PROPERTY_ID=?";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      if (insertNode == null)
         insertNode = dbConnection.prepareStatement(INSERT_NODE);
      else
         insertNode.clearParameters();

      insertNode.setString(1, getInternalId(data.getIdentifier()));
      // if root then parent identifier equals space string
      insertNode.setString(2, data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : getInternalId(data
         .getParentIdentifier()));
      insertNode.setString(3, data.getQPath().getName().getAsString());
      insertNode.setString(4, containerName);
      insertNode.setInt(5, data.getPersistedVersion());
      insertNode.setInt(6, data.getQPath().getIndex());
      insertNode.setInt(7, data.getOrderNumber());
      return insertNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException
   {
      if (insertProperty == null)
         insertProperty = dbConnection.prepareStatement(INSERT_PROPERTY);
      else
         insertProperty.clearParameters();

      insertProperty.setString(1, getInternalId(data.getIdentifier()));
      insertProperty.setString(2, getInternalId(data.getParentIdentifier()));
      insertProperty.setString(3, data.getQPath().getName().getAsString());
      insertProperty.setString(4, containerName);
      insertProperty.setInt(5, data.getPersistedVersion());
      insertProperty.setInt(6, data.getQPath().getIndex());
      insertProperty.setInt(7, data.getType());
      insertProperty.setBoolean(8, data.isMultiValued());

      return insertProperty.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addReference(PropertyData data) throws SQLException, IOException
   {
      if (insertReference == null)
         insertReference = dbConnection.prepareStatement(INSERT_REF);
      else
         insertReference.clearParameters();

      List<ValueData> values = data.getValues();
      int added = 0;
      for (int i = 0; i < values.size(); i++)
      {
         ValueData vdata = values.get(i);
         String refNodeIdentifier = new String(vdata.getAsByteArray());

         insertReference.setString(1, getInternalId(refNodeIdentifier));
         insertReference.setString(2, getInternalId(data.getIdentifier()));
         insertReference.setInt(3, i);
         added += insertReference.executeUpdate();
      }
      return added;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteReference(String propertyCid) throws SQLException
   {
      if (deleteReference == null)
         deleteReference = dbConnection.prepareStatement(DELETE_REF);
      else
         deleteReference.clearParameters();

      deleteReference.setString(1, propertyCid);
      return deleteReference.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteItemByIdentifier(String cid) throws SQLException
   {
      if (deleteItem == null)
         deleteItem = dbConnection.prepareStatement(DELETE_ITEM);
      else
         deleteItem.clearParameters();

      deleteItem.setString(1, cid);
      return deleteItem.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifier(String parentCid) throws SQLException
   {
      if (findNodesByParentId == null)
         findNodesByParentId = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID);
      else
         findNodesByParentId.clearParameters();

      findNodesByParentId.setString(1, containerName);
      findNodesByParentId.setString(2, parentCid);
      return findNodesByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findLastOrderNumberByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findLastOrderNumberByParentId == null)
         findLastOrderNumberByParentId = dbConnection.prepareStatement(FIND_LAST_ORDER_NUMBER_BY_PARENTID);
      else
         findLastOrderNumberByParentId.clearParameters();

      findLastOrderNumberByParentId.setString(1, containerName);
      findLastOrderNumberByParentId.setString(2, parentIdentifier);
      return findLastOrderNumberByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesCountByParentIdentifier(String parentCid) throws SQLException
   {
      if (findNodesCountByParentId == null)
         findNodesCountByParentId = dbConnection.prepareStatement(FIND_NODES_COUNT_BY_PARENTID);
      else
         findNodesCountByParentId.clearParameters();

      findNodesCountByParentId.setString(1, containerName);
      findNodesCountByParentId.setString(2, parentCid);
      return findNodesCountByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifier(String parentCid) throws SQLException
   {
      if (findPropertiesByParentId == null)
         findPropertiesByParentId = dbConnection.prepareStatement(FIND_PROPERTIES_BY_PARENTID);
      else
         findPropertiesByParentId.clearParameters();

      findPropertiesByParentId.setString(1, containerName);
      findPropertiesByParentId.setString(2, parentCid);
      return findPropertiesByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemByName(String parentId, String name, int index) throws SQLException
   {
      if (findItemByName == null)
         findItemByName = dbConnection.prepareStatement(FIND_ITEM_BY_NAME);
      else
         findItemByName.clearParameters();

      findItemByName.setString(1, containerName);
      findItemByName.setString(2, parentId);
      findItemByName.setString(3, name);
      findItemByName.setInt(4, index);
      return findItemByName.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findPropertyByName(String parentCid, String name) throws SQLException
   {
      if (findPropertyByName == null)
         findPropertyByName = dbConnection.prepareStatement(FIND_PROPERTY_BY_NAME);
      else
         findPropertyByName.clearParameters();

      findPropertyByName.setString(1, containerName);
      findPropertyByName.setString(2, parentCid);
      findPropertyByName.setString(3, name);
      return findPropertyByName.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemByIdentifier(String cid) throws SQLException
   {
      if (findItemById == null)
         findItemById = dbConnection.prepareStatement(FIND_ITEM_BY_ID);
      else
         findItemById.clearParameters();

      findItemById.setString(1, cid);
      return findItemById.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findReferences(String cid) throws SQLException
   {
      if (findReferences == null)
         findReferences = dbConnection.prepareStatement(FIND_REFERENCES);
      else
         findReferences.clearParameters();

      findReferences.setString(1, cid);
      findReferences.setString(2, containerName);
      return findReferences.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updateNodeByIdentifier(int version, int index, int orderNumb, String cid) throws SQLException
   {
      if (updateNode == null)
         updateNode = dbConnection.prepareStatement(UPDATE_NODE);
      else
         updateNode.clearParameters();

      updateNode.setInt(1, version);
      updateNode.setInt(2, index);
      updateNode.setInt(3, orderNumb);
      updateNode.setString(4, cid);
      return updateNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updatePropertyByIdentifier(int version, int type, String cid) throws SQLException
   {
      if (updateProperty == null)
         updateProperty = dbConnection.prepareStatement(UPDATE_PROPERTY);
      else
         updateProperty.clearParameters();

      updateProperty.setInt(1, version);
      updateProperty.setInt(2, type);
      updateProperty.setString(3, cid);
      return updateProperty.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int toOrderNum)
      throws SQLException
   {
      throw new UnsupportedOperationException("findChildNodesByParentIdentifier is not supported for old queries");
   }

   // -------- values processing ------------

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addValueData(String cid, int orderNumber, InputStream stream, int streamLength, String storageDesc)
      throws SQLException
   {

      if (insertValue == null)
         insertValue = dbConnection.prepareStatement(INSERT_VALUE);
      else
         insertValue.clearParameters();

      if (stream == null)
      {
         // [PN] store vd reference to external storage etc.
         insertValue.setNull(1, Types.BINARY);
         insertValue.setString(4, storageDesc);
      }
      else
      {
         insertValue.setBinaryStream(1, stream, streamLength);
         insertValue.setNull(4, Types.VARCHAR);
      }

      insertValue.setInt(2, orderNumber);
      insertValue.setString(3, cid);
      return insertValue.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteValueData(String cid) throws SQLException
   {
      if (deleteValue == null)
         deleteValue = dbConnection.prepareStatement(DELETE_VALUE);
      else
         deleteValue.clearParameters();

      deleteValue.setString(1, cid);
      return deleteValue.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValuesByPropertyId(String cid) throws SQLException
   {
      if (findValuesByPropertyId == null)
         findValuesByPropertyId = dbConnection.prepareStatement(FIND_VALUES_BY_PROPERTYID);
      else
         findValuesByPropertyId.clearParameters();

      findValuesByPropertyId.setString(1, cid);
      return findValuesByPropertyId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValuesStorageDescriptorsByPropertyId(String cid) throws SQLException
   {
      if (findValuesStorageDescriptorsByPropertyId == null)
         findValuesStorageDescriptorsByPropertyId =
            dbConnection.prepareStatement(FIND_VALUES_VSTORAGE_DESC_BY_PROPERTYID);
      else
         findValuesStorageDescriptorsByPropertyId.clearParameters();

      findValuesStorageDescriptorsByPropertyId.setString(1, cid);
      return findValuesStorageDescriptorsByPropertyId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValueByPropertyIdOrderNumber(String cid, int orderNumb) throws SQLException
   {
      if (findValueByPropertyIdOrderNumber == null)
      {
         findValueByPropertyIdOrderNumber = dbConnection.prepareStatement(FIND_VALUE_BY_PROPERTYID_OREDERNUMB);
      }
      else
      {
         findValueByPropertyIdOrderNumber.clearParameters();
      }

      findValueByPropertyIdOrderNumber.setString(1, cid);
      findValueByPropertyIdOrderNumber.setInt(2, orderNumb);
      return findValueByPropertyIdOrderNumber.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int renameNode(NodeData data) throws SQLException
   {
      if (renameNode == null)
      {
         renameNode = dbConnection.prepareStatement(RENAME_NODE);
      }
      else
      {
         renameNode.clearParameters();
      }

      renameNode.setString(1, data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : getInternalId(data
         .getParentIdentifier()));
      renameNode.setString(2, data.getQPath().getName().getAsString());
      renameNode.setInt(3, data.getPersistedVersion());
      renameNode.setInt(4, data.getQPath().getIndex());
      renameNode.setInt(5, data.getOrderNumber());
      renameNode.setString(6, getInternalId(data.getIdentifier()));
      return renameNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      throw new UnsupportedOperationException(
         "The method findNodesAndProperties is not supported for this type of connection use the complex queries instead");
   }

   /**
    * {@inheritDoc}
    */
   protected void deleteLockProperties() throws SQLException
   {
      PreparedStatement removeValuesStatement = null;
      PreparedStatement removeItemsStatement = null;

      try
      {
         removeValuesStatement =
            dbConnection
               .prepareStatement("DELETE FROM JCR_SVALUE WHERE PROPERTY_ID IN (SELECT ID FROM JCR_SITEM WHERE CONTAINER_NAME = ? AND "
                  + "(NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR NAME = '[http://www.jcp.org/jcr/1.0]lockOwner'))");
         removeValuesStatement.setString(1, containerName);

         removeItemsStatement =
            dbConnection.prepareStatement("DELETE FROM JCR_SITEM WHERE CONTAINER_NAME = ? AND "
               + "(NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR NAME = '[http://www.jcp.org/jcr/1.0]lockOwner')");
         removeItemsStatement.setString(1, containerName);

         removeValuesStatement.executeUpdate();
         removeItemsStatement.executeUpdate();
      }
      finally
      {
         if (removeValuesStatement != null)
         {
            try
            {
               removeValuesStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }

         if (removeItemsStatement != null)
         {
            try
            {
               removeItemsStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }
      }
   }
}
