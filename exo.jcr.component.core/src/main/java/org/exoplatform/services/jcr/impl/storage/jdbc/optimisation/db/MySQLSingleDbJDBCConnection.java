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
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * 
 * 20.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class MySQLSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   protected static final String PATTERN_ESCAPE_STRING = "\\\\";

   /**
    * MySQL Singledatabase JDBC Connection constructor.
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
   public MySQLSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
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

      FIND_ITEM_BY_NAME =
         "select * from JCR_SITEM where CONTAINER_NAME=? and PARENT_ID=? and NAME=? and I_INDEX=?"
            + " order by I_CLASS";

      FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE =
         FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE.replace("from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V",
            "from JCR_SITEM I force index (JCR_IDX_SITEM_N_ORDER_NUM), JCR_SITEM P force index (JCR_IDX_SITEM_PARENT_NAME),"
               + " JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");

      FIND_PROPERTY_BY_NAME =
         FIND_PROPERTY_BY_NAME.replace("from JCR_SITEM I, JCR_SVALUE V",
               "from JCR_SITEM I force index (JCR_IDX_SITEM_PARENT_NAME), JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");

      FIND_REFERENCES =
         FIND_REFERENCES.replace("from JCR_SREF R, JCR_SITEM P",
            "from JCR_SREF R force index (PRIMARY), JCR_SITEM P force index (PRIMARY)");

      FIND_NODES_BY_PARENTID_CQ =
         FIND_NODES_BY_PARENTID_CQ.replace("from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V",
            "from JCR_SITEM I force index (JCR_IDX_SITEM_N_ORDER_NUM), JCR_SITEM P force index (JCR_IDX_SITEM_PARENT_NAME),"
               + " JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");

      FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ =
         FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ.replace("from JCR_SITEM I, JCR_SVALUE V",
               "from JCR_SITEM I force index (JCR_IDX_SITEM_PARENT_NAME), JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");

      FIND_NODES_AND_PROPERTIES =
         FIND_NODES_AND_PROPERTIES.replace("from JCR_SITEM I", "from JCR_SITEM I force index (PRIMARY)");

      FIND_PROPERTY_BY_ID =
         FIND_PROPERTY_BY_ID.replace("from JCR_SITEM I, JCR_SVALUE V",
            "from JCR_SITEM I force index (PRIMARY), JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");
      
      FIND_NODES_BY_PARENTID_LAZILY_CQ =
         FIND_NODES_BY_PARENTID_LAZILY_CQ.replace("from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V",
            "from JCR_SITEM I force index (JCR_IDX_SITEM_N_ORDER_NUM), JCR_SITEM P force index (JCR_IDX_SITEM_PARENT_NAME),"
               + " JCR_SVALUE V force index (JCR_IDX_SVALUE_PROPERTY)");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(getInternalId(data.getParentIdentifier()));
         try
         {
            if (!item.next())
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
      return super.addNodeRecord(data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(getInternalId(data.getParentIdentifier()));
         try
         {
            if (!item.next())
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
      return super.addPropertyRecord(data);
   }

   protected String getLikeExpressionEscape()
   {
      // must be .. LIKE 'prop\\_name' ESCAPE '\\\\'
      return this.PATTERN_ESCAPE_STRING;
   }
}
