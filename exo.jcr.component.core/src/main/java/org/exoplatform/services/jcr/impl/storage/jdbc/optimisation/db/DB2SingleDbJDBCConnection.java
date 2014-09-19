/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DB2SingleDbJDBCConnection.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DB2SingleDbJDBCConnection extends SingleDbJDBCConnection
{
   /**
    * DB2 Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public DB2SingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {
      super.prepareQueries();
      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM P"
            + " join (select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from JCR_SITEM I"
            + " where I.CONTAINER_NAME=? AND I.I_CLASS=1 AND I.ID > ? order by I.ID FETCH FIRST $rowNb ROWS ONLY) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and V.PROPERTY_ID=P.ID order by J.ID";
      if (containerConfig.useSequenceForOrderNumber)
      {
         FIND_LAST_ORDER_NUMBER_BY_PARENTID = "VALUES NEXT VALUE FOR " + JCR_ITEM_SEQ;
         FIND_NODES_BY_PARENTID_LAZILY_CQ =
            "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_SVALUE V, JCR_SITEM P "
               + " join (select J.* from JCR_SITEM J where J.CONTAINER_NAME=? AND J.I_CLASS=1 and J.PARENT_ID=?"
               + " AND J.N_ORDER_NUM  >= ? order by J.N_ORDER_NUM, J.ID FETCH FIRST $rowNb ROWS ONLY) I on P.PARENT_ID = I.ID"
               + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and P.PARENT_ID=I.ID and"
               + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
               + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
               + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      if (findNodesAndProperties == null)
      {
         FIND_NODES_AND_PROPERTIES=FIND_NODES_AND_PROPERTIES.replace("$rowNb",Integer.toString(limit));
         findNodesAndProperties = dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES);
      }
      else
      {
         findNodesAndProperties.clearParameters();
      }

      findNodesAndProperties.setString(1, this.containerConfig.containerName);
      findNodesAndProperties.setString(2, getInternalId(lastNodeId));
      findNodesAndProperties.setString(3, this.containerConfig.containerName);

      return findNodesAndProperties.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int offset, int limit)
      throws SQLException
   {
      if (!containerConfig.useSequenceForOrderNumber)
      {
         return super.findChildNodesByParentIdentifier(parentCid, fromOrderNum, offset, limit);
      }
      if (findNodesByParentIdLazilyCQ == null)
      {
         FIND_NODES_BY_PARENTID_LAZILY_CQ = FIND_NODES_BY_PARENTID_LAZILY_CQ.replace("$rowNb", Integer.toString(limit));
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ);
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, this.containerConfig.containerName);
      findNodesByParentIdLazilyCQ.setString(2, parentCid);
      findNodesByParentIdLazilyCQ.setInt(3, fromOrderNum);
      findNodesByParentIdLazilyCQ.setString(4, this.containerConfig.containerName);

      return findNodesByParentIdLazilyCQ.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void startTxIfNeeded() throws SQLException
   {
      if (containerConfig.isManaged && dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }
   }
}
