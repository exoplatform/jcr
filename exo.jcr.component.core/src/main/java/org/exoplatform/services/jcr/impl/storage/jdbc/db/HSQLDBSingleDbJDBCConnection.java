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

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

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
public class HSQLDBSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   /**
    * HSQLDB Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public HSQLDBSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
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
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.CONTAINER_NAME=? and I.NAME=? and"
               + " I.ID=V.PROPERTY_ID order by V.ORDER_NUM";
      FIND_NODES_BY_PARENTID =
         "select * from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=1 and CONTAINER_NAME=?" + " order by N_ORDER_NUM";
      FIND_LAST_ORDER_NUMBER_BY_PARENTID =
         "select count(*), max(N_ORDER_NUM) from JCR_SITEM where PARENT_ID=? and I_CLASS=1 and CONTAINER_NAME=?";
      FIND_NODES_COUNT_BY_PARENTID =
         "select count(ID) from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=1 and CONTAINER_NAME=?";
      FIND_PROPERTIES_BY_PARENTID =
         "select * from JCR_SITEM" + " where PARENT_ID=? and I_CLASS=2 and CONTAINER_NAME=?" + " order by ID";

      FIND_WORKSPACE_DATA_SIZE =
         "select sum(bit_length(DATA)/8) from JCR_SITEM I, JCR_SVALUE V where I.I_CLASS=2 and I.CONTAINER_NAME=?"
            + " and I.ID=V.PROPERTY_ID";

      FIND_NODE_DATA_SIZE =
         "select sum(bit_length(DATA)/8) from JCR_SITEM I, JCR_SVALUE V where I.PARENT_ID=? and I.I_CLASS=2"
            + " and I.CONTAINER_NAME=? and I.ID=V.PROPERTY_ID";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select bit_length(DATA)/8, STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=?";

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

      findItemByName.setString(1, parentId);
      findItemByName.setString(2, this.containerConfig.containerName);
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

      findPropertyByName.setString(1, parentCid);
      findPropertyByName.setString(2, this.containerConfig.containerName);
      findPropertyByName.setString(3, name);

      return findPropertyByName.executeQuery();
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

      findNodesByParentId.setString(1, parentCid);
      findNodesByParentId.setString(2, this.containerConfig.containerName);

      return findNodesByParentId.executeQuery();
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

      findNodesCountByParentId.setString(1, parentCid);
      findNodesCountByParentId.setString(2, this.containerConfig.containerName);

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

      findPropertiesByParentId.setString(1, parentCid);
      findPropertiesByParentId.setString(2, this.containerConfig.containerName);

      return findPropertiesByParentId.executeQuery();
   }
}
