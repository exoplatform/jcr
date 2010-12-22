/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.InvalidItemStateException;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.08.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id$
 */
public class HSQLDBSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   /**
      * HSQLDB Singledatabase JDBC Connection constructor.
      * 
      * @param dbConnection
      *          JDBC connection, shoudl be opened before
      * @param readOnly
      *          boolean if true the dbConnection was marked as READ-ONLY.
      * @param containerName
      *          Workspace Storage Container name (see configuration)
      * @param valueStorageProvider
      *          External Value Storages provider
      * @param maxBufferSize
      *          Maximum buffer size (see configuration)
      * @param swapDirectory
      *          Swap directory File (see configuration)
      * @param swapCleaner
      *          Swap cleaner (internal FileCleaner).
      * @throws SQLException
      * 
      * @see org.exoplatform.services.jcr.impl.util.io.FileCleaner
      */
   public HSQLDBSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected final void prepareQueries() throws SQLException
   {

      super.prepareQueries();

      FIND_ITEM_BY_NAME =
         "select * from JCR_SITEM"
            + " where PARENT_ID=? and CONTAINER_NAME=? and NAME=? and I_INDEX=? order by I_CLASS, VERSION DESC";
      FIND_PROPERTY_BY_NAME =
         "select V.DATA"
            + " from JCR_SITEM I, JCR_SVALUE V"
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.CONTAINER_NAME=? and I.NAME=? and I.ID=V.PROPERTY_ID order by V.ORDER_NUM";
      FIND_NODES_BY_PARENTID =
         "select * from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=1 and CONTAINER_NAME=?" + " order by N_ORDER_NUM";
      FIND_NODES_COUNT_BY_PARENTID =
         "select count(ID) from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=1 and CONTAINER_NAME=?";
      FIND_PROPERTIES_BY_PARENTID =
         "select * from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=2 and CONTAINER_NAME=?" + " order by ID";
      FIND_NODES_BY_PARENTID_CQ =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA"
            + " from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V"
            + " where I.PARENT_ID=? and I.I_CLASS=1 and I.CONTAINER_NAME=? and"
            + " P.PARENT_ID=I.ID and P.I_CLASS=2 and P.CONTAINER_NAME=? and (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
            + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
      FIND_PROPERTIES_BY_PARENTID_CQ =
         "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED, V.ORDER_NUM,"
            + " V.DATA, V.STORAGE_DESC from JCR_SITEM I LEFT OUTER JOIN JCR_SVALUE V ON (V.PROPERTY_ID=I.ID)"
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.CONTAINER_NAME=? order by I.NAME";
   }

   /**
    * Use simple queries since it is much faster
    */
   @Override
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      return traverseQPathSQ(cpid);
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

      findItemByName.setString(1, parentId);
      findItemByName.setString(2, containerName);
      findItemByName.setString(3, name);
      findItemByName.setInt(4, index);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findItemByName.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
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

      findPropertyByName.setString(1, parentCid);
      findPropertyByName.setString(2, containerName);
      findPropertyByName.setString(3, name);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findPropertyByName.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
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

      findNodesByParentId.setString(1, parentCid);
      findNodesByParentId.setString(2, containerName);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findNodesByParentId.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
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

      findNodesCountByParentId.setString(1, parentCid);
      findNodesCountByParentId.setString(2, containerName);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findNodesCountByParentId.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
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

      findPropertiesByParentId.setString(1, parentCid);
      findPropertiesByParentId.setString(2, containerName);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findPropertiesByParentId.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException
   {
      if (findNodesByParentIdCQ == null)
         findNodesByParentIdCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_CQ);
      else
         findNodesByParentIdCQ.clearParameters();

      findNodesByParentIdCQ.setString(1, parentIdentifier);
      findNodesByParentIdCQ.setString(2, containerName);
      findNodesByParentIdCQ.setString(3, containerName);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findNodesByParentIdCQ.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException
   {
      if (findPropertiesByParentIdCQ == null)
         findPropertiesByParentIdCQ = dbConnection.prepareStatement(FIND_PROPERTIES_BY_PARENTID_CQ);
      else
         findPropertiesByParentIdCQ.clearParameters();

      findPropertiesByParentIdCQ.setString(1, parentIdentifier);
      findPropertiesByParentIdCQ.setString(2, containerName);

      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return findPropertiesByParentIdCQ.executeQuery();
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(final NodeData data) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.addNodeRecord(data);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(final PropertyData prop) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.addPropertyRecord(prop);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemByIdentifier(final String identifier) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findItemByIdentifier(identifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addReference(final PropertyData data) throws SQLException, IOException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.addReference(data);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int renameNode(final NodeData data) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.renameNode(data);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteReference(final String propertyIdentifier) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.deleteReference(propertyIdentifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findReferences(final String nodeIdentifier) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findReferences(nodeIdentifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteItemByIdentifier(final String identifier) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.deleteItemByIdentifier(identifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updateNodeByIdentifier(final int version, final int index, final int orderNumb, final String identifier)
      throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.updateNodeByIdentifier(version, index, orderNumb, identifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updatePropertyByIdentifier(final int version, final int type, final String identifier)
      throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.updatePropertyByIdentifier(version, type, identifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   // -------- values processing ------------
   /**
    * {@inheritDoc}
    */
   @Override
   protected int addValueData(final String cid, final int orderNumber, final InputStream stream,
      final int streamLength, final String storageId) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.addValueData(cid, orderNumber, stream, streamLength, storageId);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteValueData(final String cid) throws SQLException
   {
      PrivilegedExceptionAction<Integer> action = new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.deleteValueData(cid);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValuesByPropertyId(final String cid) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findValuesByPropertyId(cid);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValuesStorageDescriptorsByPropertyId(final String cid) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findValuesStorageDescriptorsByPropertyId(cid);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValueByPropertyIdOrderNumber(final String cid, final int orderNumb) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findValueByPropertyIdOrderNumber(cid, orderNumb);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemQPathByIdentifierCQ(final String identifier) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findItemQPathByIdentifierCQ(identifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodeMainPropertiesByParentIdentifierCQ(final String parentIdentifier) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBSingleDbJDBCConnection.super.findNodeMainPropertiesByParentIdentifierCQ(parentIdentifier);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }
}
