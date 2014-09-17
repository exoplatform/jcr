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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DB2MultiDbJDBCConnection.ajva 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DB2MultiDbJDBCConnection extends MultiDbJDBCConnection
{
   /**
    * DB2 Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public DB2MultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
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
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from " + JCR_VALUE + " V, " + JCR_ITEM + " P"
            + " join (select A.* from (select Row_Number() over (order by I.ID) as r__, I.ID,"
            + " I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from " + JCR_ITEM
            + " I where I.I_CLASS=1) as A where A.r__ <= ? and A.r__ > ?) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and V.PROPERTY_ID=P.ID order by J.ID";
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

      findNodesAndProperties.setInt(1, offset + limit);
      findNodesAndProperties.setInt(2, offset);

      return findNodesAndProperties.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void startTxIfNeeded() throws SQLException
   {
      if (containerConfig.isManaged  && dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }
   }
}
