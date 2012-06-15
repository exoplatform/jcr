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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.CQJDBCStorageConnection;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.jcr.RepositoryException;

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
public class SingleDbJDBCConnection extends CQJDBCStorageConnection
{

   protected static final String FIND_NODES_BY_PARENTID_CQ_QUERY =
      "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V"
         + " where I.I_CLASS=1 and I.CONTAINER_NAME=? and I.PARENT_ID=? and"
         + " P.I_CLASS=2 and P.CONTAINER_NAME=? and P.PARENT_ID=I.ID and"
         + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
         + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
         + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
         + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
         + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";

   protected static final String FIND_PROPERTIES_BY_PARENTID_CQ_QUERY =
      "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED, V.ORDER_NUM,"
         + " V.DATA, V.STORAGE_DESC from JCR_SITEM I LEFT OUTER JOIN JCR_SVALUE V ON (V.PROPERTY_ID=I.ID)"
         + " where I.I_CLASS=2 and I.CONTAINER_NAME=? and I.PARENT_ID=? order by I.NAME";

   protected static final String FIND_ITEM_QPATH_BY_ID_CQ_QUERY = "select I.ID, I.PARENT_ID, I.NAME, I.I_INDEX"
      + " from JCR_SITEM I, (SELECT ID, PARENT_ID from JCR_SITEM where ID=?) J"
      + " where I.ID = J.ID or I.ID = J.PARENT_ID";

   protected static final String PATTERN_ESCAPE_STRING = "\\"; //valid for HSQL, Sybase, DB2, MSSQL, ORACLE

   /**
    * Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public SingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {

      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getInternalId(final String identifier)
   {
      return this.containerConfig.containerName + identifier;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getIdentifier(final String internalId)
   {

      if (internalId == null)
      {
         return null;
      }

      return internalId.substring(this.containerConfig.containerName.length());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {

      FIND_ITEM_BY_ID = "select * from JCR_SITEM where ID=?";

      FIND_ITEM_BY_NAME =
         "select * from JCR_SITEM"
            + " where CONTAINER_NAME=? and PARENT_ID=? and NAME=? and I_INDEX=? order by I_CLASS, VERSION DESC";

      FIND_PROPERTY_BY_NAME =
         "select V.DATA from JCR_SITEM I, JCR_SVALUE V"
            + " where I.I_CLASS=2 and I.CONTAINER_NAME=? and I.PARENT_ID=? and I.NAME=? and"
            + " I.ID=V.PROPERTY_ID order by V.ORDER_NUM";

      FIND_REFERENCES =
         "select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME" + " from JCR_SREF R, JCR_SITEM P"
            + " where R.NODE_ID=? and P.CONTAINER_NAME=? and P.ID=R.PROPERTY_ID and P.I_CLASS=2";

      FIND_VALUES_BY_PROPERTYID =
         "select PROPERTY_ID, ORDER_NUM, DATA, STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=? order by ORDER_NUM";

      FIND_VALUES_VSTORAGE_DESC_BY_PROPERTYID = "select distinct STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=?";

      FIND_NODES_BY_PARENTID =
         "select * from JCR_SITEM" + " where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?" + " order by N_ORDER_NUM";

      FIND_NODES_BY_PARENTID_CQ = FIND_NODES_BY_PARENTID_CQ_QUERY;

      FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ =
         "select I.NAME, V.DATA, V.ORDER_NUM from JCR_SITEM I, JCR_SVALUE V"
            + " where I.I_CLASS=2 and I.CONTAINER_NAME=? and I.PARENT_ID=? and"
            + " (I.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
            + " I.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
            + " I.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
            + " I.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions') and I.ID=V.PROPERTY_ID";

      FIND_ITEM_QPATH_BY_ID_CQ = FIND_ITEM_QPATH_BY_ID_CQ_QUERY;

      FIND_LAST_ORDER_NUMBER_BY_PARENTID =
         "select count(*), max(N_ORDER_NUM) from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

      FIND_NODES_COUNT_BY_PARENTID =
         "select count(ID) from JCR_SITEM" + " where I_CLASS=1 and CONTAINER_NAME=? and PARENT_ID=?";

      FIND_PROPERTIES_BY_PARENTID =
         "select * from JCR_SITEM" + " where I_CLASS=2 and CONTAINER_NAME=? and PARENT_ID=?" + " order by NAME";

      FIND_PROPERTIES_BY_PARENTID_CQ = FIND_PROPERTIES_BY_PARENTID_CQ_QUERY;
      FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE =
         "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE,"
            + " I.P_MULTIVALUED, V.ORDER_NUM, V.DATA, V.STORAGE_DESC"
            + " from JCR_SITEM I LEFT OUTER JOIN JCR_SVALUE V ON (V.PROPERTY_ID=I.ID)";

      FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V";

      FIND_MAX_PROPERTY_VERSIONS =
         "select max(VERSION) FROM JCR_SITEM WHERE PARENT_ID=? and CONTAINER_NAME=? and NAME=? and I_INDEX=? and I_CLASS=2";

      INSERT_NODE =
         "insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM) VALUES(?,?,?,?,?,"
            + I_CLASS_NODE + ",?,?)";
      INSERT_PROPERTY =
         "insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED)"
            + " VALUES(?,?,?,?,?," + I_CLASS_PROPERTY + ",?,?,?)";

      INSERT_VALUE = "insert into JCR_SVALUE(DATA, ORDER_NUM, PROPERTY_ID, STORAGE_DESC) VALUES(?,?,?,?)";
      INSERT_REF = "insert into JCR_SREF(NODE_ID, PROPERTY_ID, ORDER_NUM) VALUES(?,?,?)";

      RENAME_NODE = "update JCR_SITEM set PARENT_ID=?, NAME=?, VERSION=?, I_INDEX=?, N_ORDER_NUM=? where ID=?";

      UPDATE_NODE = "update JCR_SITEM set VERSION=?, I_INDEX=?, N_ORDER_NUM=? where ID=?";
      UPDATE_PROPERTY = "update JCR_SITEM set VERSION=?, P_TYPE=? where ID=?";

      DELETE_ITEM = "delete from JCR_SITEM where ID=?";
      DELETE_VALUE = "delete from JCR_SVALUE where PROPERTY_ID=?";
      DELETE_REF = "delete from JCR_SREF where PROPERTY_ID=?";

      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM P"
            + " join (select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from JCR_SITEM I"
            + " where I.CONTAINER_NAME=? AND I.I_CLASS=1 AND I.ID > ? order by I.ID LIMIT ? OFFSET ?) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and V.PROPERTY_ID=P.ID order by J.ID";

      FIND_PROPERTY_BY_ID =
         "select I.P_TYPE, V.STORAGE_DESC from JCR_SITEM I, JCR_SVALUE V where I.ID = ? and V.PROPERTY_ID = I.ID";
      DELETE_VALUE_BY_ORDER_NUM = "delete from JCR_SVALUE where PROPERTY_ID=? and ORDER_NUM >= ?";
      UPDATE_VALUE = "update JCR_SVALUE set DATA=?, STORAGE_DESC=? where PROPERTY_ID=? and ORDER_NUM=?";

      FIND_NODES_BY_PARENTID_LAZILY_CQ =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V"
            + " where I.I_CLASS=1 and I.CONTAINER_NAME=? and I.PARENT_ID=? and I.N_ORDER_NUM >= ? and "
            + " I.N_ORDER_NUM <= ? and P.I_CLASS=2 and P.CONTAINER_NAME=? and P.PARENT_ID=I.ID and"
            + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
            + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
            + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";

      FIND_ACL_HOLDERS =
         "select I.PARENT_ID, I.P_TYPE" + " from JCR_SITEM I where I.I_CLASS=2 and I.CONTAINER_NAME=?"
            + " and (I.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner'"
            + " or I.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')";

      FIND_NODES_COUNT = "select count(*) from JCR_SITEM I where I.I_CLASS=1 and I.CONTAINER_NAME=?";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      if (insertNode == null)
      {
         insertNode = dbConnection.prepareStatement(INSERT_NODE);
      }
      else
      {
         insertNode.clearParameters();
      }

      insertNode.setString(1, getInternalId(data.getIdentifier()));
      // if root then parent identifier equals space string
      insertNode.setString(2,
         data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : getInternalId(data.getParentIdentifier()));
      insertNode.setString(3, data.getQPath().getName().getAsString());
      insertNode.setString(4, this.containerConfig.containerName);
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
      {
         insertProperty = dbConnection.prepareStatement(INSERT_PROPERTY);
      }
      else
      {
         insertProperty.clearParameters();
      }

      insertProperty.setString(1, getInternalId(data.getIdentifier()));
      insertProperty.setString(2, getInternalId(data.getParentIdentifier()));
      insertProperty.setString(3, data.getQPath().getName().getAsString());
      insertProperty.setString(4, this.containerConfig.containerName);
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
      {
         insertReference = dbConnection.prepareStatement(INSERT_REF);
      }
      else
      {
         insertReference.clearParameters();
      }

      List<ValueData> values = data.getValues();
      int added = 0;
      for (int i = 0; i < values.size(); i++)
      {
         ValueData vdata = values.get(i);

         String refNodeIdentifier;
         try
         {
            refNodeIdentifier = ValueDataUtil.getString(vdata);
         }
         catch (RepositoryException e)
         {
            throw new IOException(e.getMessage(), e);
         }

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
      {
         deleteReference = dbConnection.prepareStatement(DELETE_REF);
      }
      else
      {
         deleteReference.clearParameters();
      }

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
      {
         deleteItem = dbConnection.prepareStatement(DELETE_ITEM);
      }
      else
      {
         deleteItem.clearParameters();
      }

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
      {
         findNodesByParentId = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID);
      }
      else
      {
         findNodesByParentId.clearParameters();
      }

      findNodesByParentId.setString(1, this.containerConfig.containerName);
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
      {
         findLastOrderNumberByParentId = dbConnection.prepareStatement(FIND_LAST_ORDER_NUMBER_BY_PARENTID);
      }
      else
      {
         findLastOrderNumberByParentId.clearParameters();
      }

      findLastOrderNumberByParentId.setString(1, this.containerConfig.containerName);
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
      {
         findNodesCountByParentId = dbConnection.prepareStatement(FIND_NODES_COUNT_BY_PARENTID);
      }
      else
      {
         findNodesCountByParentId.clearParameters();
      }

      findNodesCountByParentId.setString(1, this.containerConfig.containerName);
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
      {
         findPropertiesByParentId = dbConnection.prepareStatement(FIND_PROPERTIES_BY_PARENTID);
      }
      else
      {
         findPropertiesByParentId.clearParameters();
      }

      findPropertiesByParentId.setString(1, this.containerConfig.containerName);
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
      {
         findItemByName = dbConnection.prepareStatement(FIND_ITEM_BY_NAME);
      }
      else
      {
         findItemByName.clearParameters();
      }

      findItemByName.setString(1, this.containerConfig.containerName);
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
      {
         findPropertyByName = dbConnection.prepareStatement(FIND_PROPERTY_BY_NAME);
      }
      else
      {
         findPropertyByName.clearParameters();
      }

      findPropertyByName.setString(1, this.containerConfig.containerName);
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
      {
         findItemById = dbConnection.prepareStatement(FIND_ITEM_BY_ID);
      }
      else
      {
         findItemById.clearParameters();
      }

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
      {
         findReferences = dbConnection.prepareStatement(FIND_REFERENCES);
      }
      else
      {
         findReferences.clearParameters();
      }

      findReferences.setString(1, cid);
      findReferences.setString(2, this.containerConfig.containerName);
      return findReferences.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updateNodeByIdentifier(int version, int index, int orderNumb, String cid) throws SQLException
   {
      if (updateNode == null)
      {
         updateNode = dbConnection.prepareStatement(UPDATE_NODE);
      }
      else
      {
         updateNode.clearParameters();
      }

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
      {
         updateProperty = dbConnection.prepareStatement(UPDATE_PROPERTY);
      }
      else
      {
         updateProperty.clearParameters();
      }

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
      if (findNodesByParentIdLazilyCQ == null)
      {
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ);
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, this.containerConfig.containerName);
      findNodesByParentIdLazilyCQ.setString(2, parentCid);
      findNodesByParentIdLazilyCQ.setInt(3, fromOrderNum);
      findNodesByParentIdLazilyCQ.setInt(4, toOrderNum);
      findNodesByParentIdLazilyCQ.setString(5, this.containerConfig.containerName);

      return findNodesByParentIdLazilyCQ.executeQuery();
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
      {
         insertValue = dbConnection.prepareStatement(INSERT_VALUE);
      }
      else
      {
         insertValue.clearParameters();
      }

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
      {
         deleteValue = dbConnection.prepareStatement(DELETE_VALUE);
      }
      else
      {
         deleteValue.clearParameters();
      }

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
      {
         findValuesByPropertyId = dbConnection.prepareStatement(FIND_VALUES_BY_PROPERTYID);
      }
      else
      {
         findValuesByPropertyId.clearParameters();
      }

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
      {
         findValuesStorageDescriptorsByPropertyId =
            dbConnection.prepareStatement(FIND_VALUES_VSTORAGE_DESC_BY_PROPERTYID);
      }
      else
      {
         findValuesStorageDescriptorsByPropertyId.clearParameters();
      }

      findValuesStorageDescriptorsByPropertyId.setString(1, cid);
      return findValuesStorageDescriptorsByPropertyId.executeQuery();
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

      renameNode.setString(1,
         data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : getInternalId(data.getParentIdentifier()));
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
   protected ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException
   {
      if (findNodesByParentIdCQ == null)
      {
         findNodesByParentIdCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_CQ);
      }
      else
      {
         findNodesByParentIdCQ.clearParameters();
      }

      findNodesByParentIdCQ.setString(1, this.containerConfig.containerName);
      findNodesByParentIdCQ.setString(2, parentIdentifier);
      findNodesByParentIdCQ.setString(3, this.containerConfig.containerName);
      return findNodesByParentIdCQ.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier, List<QPathEntryFilter> pattern)
      throws SQLException
   {
      if (pattern.isEmpty())
      {
         throw new SQLException("Pattern list is empty.");
      }
      else
      {
         if (findNodesByParentIdAndComplexPatternCQ == null)
         {
            findNodesByParentIdAndComplexPatternCQ = dbConnection.createStatement();
         }
         //create query from list
         StringBuilder query = new StringBuilder(FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE);
         query.append(" where I.I_CLASS=1 and I.CONTAINER_NAME='");
         query.append(this.containerConfig.containerName);
         query.append("' and I.PARENT_ID='");
         query.append(parentIdentifier);
         query.append("' and ( ");
         appendPattern(query, pattern.get(0).getQPathEntry(), true);
         for (int i = 1; i < pattern.size(); i++)
         {
            query.append(" or ");
            appendPattern(query, pattern.get(i).getQPathEntry(), true);
         }
         query.append(" ) and P.I_CLASS=2 and P.CONTAINER_NAME='");
         query.append(this.containerConfig.containerName);
         query.append("' and P.PARENT_ID=I.ID and (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType'");
         query.append(" or P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes'");
         query.append(" or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner'");
         query.append(" or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')");
         query.append(" and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID");

         return findNodesByParentIdAndComplexPatternCQ.executeQuery(query.toString());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException
   {
      if (findPropertiesByParentIdCQ == null)
      {
         findPropertiesByParentIdCQ = dbConnection.prepareStatement(FIND_PROPERTIES_BY_PARENTID_CQ);
      }
      else
      {
         findPropertiesByParentIdCQ.clearParameters();
      }

      findPropertiesByParentIdCQ.setString(1, this.containerConfig.containerName);
      findPropertiesByParentIdCQ.setString(2, parentIdentifier);
      return findPropertiesByParentIdCQ.executeQuery();

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifierCQ(String parentCid, List<QPathEntryFilter> pattern)
      throws SQLException
   {
      if (pattern.isEmpty())
      {
         throw new SQLException("Pattern list is empty.");
      }
      else
      {
         if (findPropertiesByParentIdAndComplexPatternCQ == null)
         {
            findPropertiesByParentIdAndComplexPatternCQ = dbConnection.createStatement();
         }
         //create query from list
         StringBuilder query = new StringBuilder(FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE);
         query.append(" where I.I_CLASS=2 and I.CONTAINER_NAME='");
         query.append(this.containerConfig.containerName);
         query.append("' and I.PARENT_ID='");
         query.append(parentCid);
         query.append("' and ( ");
         appendPattern(query, pattern.get(0).getQPathEntry(), false);
         for (int i = 1; i < pattern.size(); i++)
         {
            query.append(" or ");
            appendPattern(query, pattern.get(i).getQPathEntry(), false);
         }
         query.append(" ) order by I.NAME");

         return findPropertiesByParentIdAndComplexPatternCQ.executeQuery(query.toString());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodeMainPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException
   {
      if (findNodeMainPropertiesByParentIdentifierCQ == null)
      {
         findNodeMainPropertiesByParentIdentifierCQ =
            dbConnection.prepareStatement(FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ);
      }
      else
      {
         findNodeMainPropertiesByParentIdentifierCQ.clearParameters();
      }

      findNodeMainPropertiesByParentIdentifierCQ.setString(1, this.containerConfig.containerName);
      findNodeMainPropertiesByParentIdentifierCQ.setString(2, parentIdentifier);
      return findNodeMainPropertiesByParentIdentifierCQ.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemQPathByIdentifierCQ(String identifier) throws SQLException
   {
      if (findItemQPathByIdentifierCQ == null)
      {
         findItemQPathByIdentifierCQ = dbConnection.prepareStatement(FIND_ITEM_QPATH_BY_ID_CQ);
      }
      else
      {
         findItemQPathByIdentifierCQ.clearParameters();
      }

      findItemQPathByIdentifierCQ.setString(1, identifier);
      return findItemQPathByIdentifierCQ.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      if (findNodesAndProperties == null)
      {
         findNodesAndProperties = dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES);
      }
      else
      {
         findNodesAndProperties.clearParameters();
      }

      findNodesAndProperties.setString(1, this.containerConfig.containerName);
      findNodesAndProperties.setString(2, getInternalId(lastNodeId));
      findNodesAndProperties.setInt(3, limit);
      findNodesAndProperties.setInt(4, offset);
      findNodesAndProperties.setString(5, this.containerConfig.containerName);

      return findNodesAndProperties.executeQuery();
   }

   @Override
   protected int deleteValueDataByOrderNum(String id, int orderNum) throws SQLException
   {
      if (deleteValueDataByOrderNum == null)
      {
         deleteValueDataByOrderNum = dbConnection.prepareStatement(DELETE_VALUE_BY_ORDER_NUM);
      }
      else
      {
         deleteValueDataByOrderNum.clearParameters();
      }

      deleteValueDataByOrderNum.setString(1, id);
      deleteValueDataByOrderNum.setInt(2, orderNum);
      return deleteValueDataByOrderNum.executeUpdate();
   }

   @Override
   protected ResultSet findPropertyById(String id) throws SQLException
   {
      if (findPropertyById == null)
      {
         findPropertyById = dbConnection.prepareStatement(FIND_PROPERTY_BY_ID);
      }
      else
      {
         findPropertyById.clearParameters();
      }

      findPropertyById.setString(1, id);
      return findPropertyById.executeQuery();
   }

   @Override
   protected int updateValueData(String cid, int orderNumber, InputStream stream, int streamLength, String storageDesc)
      throws SQLException
   {

      if (updateValue == null)
      {
         updateValue = dbConnection.prepareStatement(UPDATE_VALUE);
      }
      else
      {
         updateValue.clearParameters();
      }

      if (stream == null)
      {
         // [PN] store vd reference to external storage etc.
         updateValue.setNull(1, Types.BINARY);
         updateValue.setString(2, storageDesc);
      }
      else
      {
         updateValue.setBinaryStream(1, stream, streamLength);
         updateValue.setNull(2, Types.VARCHAR);
      }

      updateValue.setString(3, cid);
      updateValue.setInt(4, orderNumber);
      return updateValue.executeUpdate();
   }

   /**
    * Replace underscore in pattern with escaped symbol. Replace jcr-wildcard '*' with sql-wildcard '%'.
    * 
    * @param pattern
    * @return pattern with escaped underscore and fixed wildcard symbols
    */
   protected String fixEscapeSymbols(String pattern)
   {
      char[] chars = pattern.toCharArray();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < chars.length; i++)
      {
         switch (chars[i])
         {
            case '*' :
               sb.append('%');
               break;
            case '_' :
            case '%' :
               sb.append(getWildcardEscapeSymbold());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }

   /**
    * Append pattern expression.
    * Appends String "I.NAME LIKE 'escaped pattern' ESCAPE 'escapeString'" or "I.NAME='pattern'"
    * to String builder sb.
    * 
    * @param sb StringBuilder
    * @param indexConstraint 
    * @param pattern
    */
   protected void appendPattern(StringBuilder sb, QPathEntry entry, boolean indexConstraint)
   {
      String pattern = entry.getAsString(false);
      sb.append("(I.NAME");
      if (pattern.contains("*"))
      {
         sb.append(" LIKE '");
         sb.append(fixEscapeSymbols(pattern));
         sb.append("' ESCAPE '");
         sb.append(getLikeExpressionEscape());
         sb.append("'");
      }
      else
      {
         sb.append("='");
         sb.append(pattern);
         sb.append("'");
      }

      if (indexConstraint && entry.getIndex() != -1)
      {
         sb.append(" and I.I_INDEX=");
         sb.append(entry.getIndex());
      }
      sb.append(")");
   }

   protected String getWildcardEscapeSymbold()
   {
      return PATTERN_ESCAPE_STRING;
   }

   protected String getLikeExpressionEscape()
   {
      return PATTERN_ESCAPE_STRING;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findACLHolders() throws SQLException
   {
      if (findACLHolders == null)
      {
         findACLHolders = dbConnection.prepareStatement(FIND_ACL_HOLDERS);
      }
      else
      {
         findACLHolders.clearParameters();
      }

      findACLHolders.setString(1, this.containerConfig.containerName);

      return findACLHolders.executeQuery();
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
            dbConnection.prepareStatement("DELETE FROM JCR_SVALUE WHERE PROPERTY_ID IN (SELECT ID FROM JCR_SITEM"
               + " WHERE CONTAINER_NAME = ? AND (NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR"
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockOwner'))");
         removeValuesStatement.setString(1, this.containerConfig.containerName);

         removeItemsStatement =
            dbConnection.prepareStatement("DELETE FROM JCR_SITEM WHERE CONTAINER_NAME = ? AND"
               + " (NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR"
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockOwner')");
         removeItemsStatement.setString(1, this.containerConfig.containerName);

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

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesCount() throws SQLException
   {
      if (findNodesCount == null)
      {
         findNodesCount = dbConnection.prepareStatement(FIND_NODES_COUNT);
      }
      else
      {
         findNodesCount.clearParameters();
      }

      findNodesCount.setString(1, this.containerConfig.containerName);

      return findNodesCount.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findMaxPropertyVersion(String parentId, String name, int index) throws SQLException
   {
      if (findMaxPropertyVersions == null)
      {
         findMaxPropertyVersions = dbConnection.prepareStatement(FIND_MAX_PROPERTY_VERSIONS);
      }

      findMaxPropertyVersions.setString(1, getInternalId(parentId));
      findMaxPropertyVersions.setString(2, containerConfig.containerName);
      findMaxPropertyVersions.setString(3, name);
      findMaxPropertyVersions.setInt(4, index);

      return findMaxPropertyVersions.executeQuery();
   }
}
