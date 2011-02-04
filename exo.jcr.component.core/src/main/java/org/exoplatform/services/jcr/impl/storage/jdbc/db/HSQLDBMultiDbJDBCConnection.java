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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.08.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id:$
 */
public class HSQLDBMultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
    * HSQLDB Multidatabase JDBC Connection constructor.
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
   public HSQLDBMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {

      super.prepareQueries();
      FIND_PROPERTY_BY_NAME =
         "select V.DATA" + " from JCR_MITEM I, JCR_MVALUE V"
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.NAME=? and I.ID=V.PROPERTY_ID order by V.ORDER_NUM";
      FIND_NODES_BY_PARENTID = "select * from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=1" + " order by N_ORDER_NUM";
      FIND_NODES_COUNT_BY_PARENTID = "select count(ID) from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=1";
      FIND_PROPERTIES_BY_PARENTID = "select * from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=2" + " order by ID";
      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_MVALUE V, JCR_MITEM P"
            + " join (select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from JCR_MITEM I"
            + " where I.I_CLASS=1 order by I.ID LIMIT ? OFFSET ?) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and V.PROPERTY_ID=P.ID order by J.ID";
   }

   @Override
   protected int addNodeRecord(final NodeData data) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.addNodeRecord(data);
         }
      });
   }

   @Override
   protected int addPropertyRecord(final PropertyData data) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.addPropertyRecord(data);
         }
      });
   }

   @Override
   protected int addReference(final PropertyData data) throws SQLException, IOException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.addReference(data);
         }
      });
   }

   @Override
   protected int addValueData(final String cid, final int orderNumber, final InputStream stream,
      final int streamLength, final String storageDesc) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.addValueData(cid, orderNumber, stream, streamLength, storageDesc);

         }
      });
   }

   @Override
   protected int deleteItemByIdentifier(final String identifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.deleteItemByIdentifier(identifier);
         }
      });
   }

   @Override
   protected int deleteReference(final String propertyIdentifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.deleteReference(propertyIdentifier);

         }
      });
   }

   @Override
   protected int deleteValueData(final String cid) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.deleteValueData(cid);
         }
      });
   }

   @Override
   protected ResultSet findChildNodesByParentIdentifier(final String parentIdentifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findChildNodesByParentIdentifier(parentIdentifier);
         }
      });
   }

   @Override
   protected ResultSet findChildNodesCountByParentIdentifier(final String parentIdentifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findChildNodesCountByParentIdentifier(parentIdentifier);
         }
      });
   }

   @Override
   protected ResultSet findChildPropertiesByParentIdentifier(final String parentIdentifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findChildPropertiesByParentIdentifier(parentIdentifier);
         }
      });
   }

   @Override
   protected ResultSet findItemByIdentifier(final String identifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findItemByIdentifier(identifier);
         }
      });
   }

   @Override
   protected ResultSet findItemByName(final String parentId, final String name, final int index) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findItemByName(parentId, name, index);
         }
      });
   }

   @Override
   protected ResultSet findPropertyByName(final String parentId, final String name) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findPropertyByName(parentId, name);
         }
      });
   }

   @Override
   protected ResultSet findReferences(final String nodeIdentifier) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findReferences(nodeIdentifier);
         }
      });
   }

   @Override
   protected ResultSet findValueByPropertyIdOrderNumber(final String cid, final int orderNumb) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findValueByPropertyIdOrderNumber(cid, orderNumb);
         }
      });
   }

   @Override
   protected ResultSet findValuesByPropertyId(final String cid) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findValuesByPropertyId(cid);
         }
      });
   }

   @Override
   protected ResultSet findValuesStorageDescriptorsByPropertyId(final String cid) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findValuesStorageDescriptorsByPropertyId(cid);

         }
      });
   }

   @Override
   protected int renameNode(final NodeData data) throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.renameNode(data);

         }
      });
   }

   @Override
   protected int updateNodeByIdentifier(final int version, final int index, final int orderNumb, final String identifier)
      throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.updateNodeByIdentifier(version, index, orderNumb, identifier);
         }
      });
   }

   @Override
   protected int updatePropertyByIdentifier(final int version, final int type, final String identifier)
      throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.updatePropertyByIdentifier(version, type, identifier);
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(final int offset, final int limit) throws SQLException
   {
      PrivilegedExceptionAction<ResultSet> action = new PrivilegedExceptionAction<ResultSet>()
      {
         public ResultSet run() throws Exception
         {
            return HSQLDBMultiDbJDBCConnection.super.findNodesAndProperties(offset, limit);
         }
      };
      return SecurityHelper.doPrivilegedSQLExceptionAction(action);
   }
}
