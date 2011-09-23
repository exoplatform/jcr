/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.SybaseJDBCConnectionHelper.EmptyResultSet;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SybaseSingleDBJDBCConnection.java 111 4.05.2011 serg $
 */
public class SybaseSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   /**
    * SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE
    */
   protected String SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE;

   /**
    * COUNT_NODES_IN_TEMPORARY_TABLE
    */
   protected String COUNT_NODES_IN_TEMPORARY_TABLE;

   /**
    * SELECT_LIMIT_NODES_FROM_TEMPORARY_TABLE
    */
   protected String SELECT_LIMIT_NODES_FROM_TEMPORARY_TABLE;

   /**
    * DELETE_TEMPORARY_TABLE_A
    */
   protected String DELETE_TEMPORARY_TABLE_A;

   /**
    * DELETE_TEMPORARY_TABLE_B
    */
   protected String DELETE_TEMPORARY_TABLE_B;

   protected PreparedStatement selectLimitOffsetNodesIntoTemporaryTable;

   protected PreparedStatement selectLimitNodesInTemporaryTable;

   protected PreparedStatement deleteTemporaryTableA;

   protected PreparedStatement deleteTemporaryTableB;

   protected PreparedStatement countNodesInTemporaryTable;

   public SybaseSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
            ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory,
            FileCleaner swapCleaner) throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
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
            case '[' : //Sybase pattern special symbol
            case ']' : //Sybase pattern special symbol
               sb.append(getWildcardEscapeSymbold());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }

   /**
    * {@inheritDoc}
    */
   protected void prepareQueries() throws SQLException
   {

      super.prepareQueries();

      SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE =
               "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM into "
                        + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME
                        + " from JCR_SITEM I (index JCR_PK_SITEM) where I.CONTAINER_NAME=? AND I.I_CLASS=1 AND I.ID > ? order by I.ID ASC";

      COUNT_NODES_IN_TEMPORARY_TABLE = "select count(*) from " + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME;

      SELECT_LIMIT_NODES_FROM_TEMPORARY_TABLE =
               "select * into " + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME + " from "
                        + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + " order by "
                        + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + ".ID DESC";

      FIND_NODES_AND_PROPERTIES =
               "select " + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME
                        + ".*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
                        + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM P, "
                        + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME + " where P.PARENT_ID = "
                        + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME
                        + ".ID and P.I_CLASS=2 and P.CONTAINER_NAME=? and V.PROPERTY_ID=P.ID "
                        + "order by " + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME + ".ID";

      DELETE_TEMPORARY_TABLE_A = "drop table " + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME;

      DELETE_TEMPORARY_TABLE_B = "drop table " + SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME;
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      String tempTableAName = "#a" + IdGenerator.generate();
      String tempTableBName = "#b" + IdGenerator.generate();

      boolean tempTableACreated = false;
      boolean tempTableBCreated = false;

      try
      {
         // the Sybase is not allowed DDL query (CREATE TABLE, DROP TABLE, etc. ) within a multi-statement transaction
         if (!dbConnection.getAutoCommit())
         {
            dbConnection.setAutoCommit(true);
         }

         selectLimitOffsetNodesIntoTemporaryTable =
                  dbConnection.prepareStatement(SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName));

         countNodesInTemporaryTable =
                  dbConnection.prepareStatement(COUNT_NODES_IN_TEMPORARY_TABLE.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName));

         selectLimitNodesInTemporaryTable =
                  dbConnection.prepareStatement(SELECT_LIMIT_NODES_FROM_TEMPORARY_TABLE.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName).replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME, tempTableBName));

         if (findNodesAndProperties != null)
         {
            findNodesAndProperties.close();
         }

         findNodesAndProperties =
                  dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME, tempTableBName));

         deleteTemporaryTableA =
                  dbConnection.prepareStatement(DELETE_TEMPORARY_TABLE_A.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName));

         deleteTemporaryTableB =
                  dbConnection.prepareStatement(DELETE_TEMPORARY_TABLE_B.replaceAll(
                           SybaseJDBCConnectionHelper.TEMP_B_TABLE_NAME, tempTableBName));

            
         selectLimitOffsetNodesIntoTemporaryTable.setMaxRows(limit + offset);
         selectLimitOffsetNodesIntoTemporaryTable.setString(1, containerName);
         selectLimitOffsetNodesIntoTemporaryTable.setString(2, getInternalId(lastNodeId));
         selectLimitOffsetNodesIntoTemporaryTable.execute();

         tempTableACreated = true;

         ResultSet nodesCountInTemporaryTable = countNodesInTemporaryTable.executeQuery();

         if (!nodesCountInTemporaryTable.next())
         {
            throw new SQLException("Can not count nodes in temporary table.");
         }

         int count = nodesCountInTemporaryTable.getInt(1);

         // define newLimit if number of records in temporary table #tempA is equal offset + limit 
         int newLimit = limit;

         if (offset > count)
         {
            // return empty ResultSet because there are no enough nodes to return
            return new EmptyResultSet();
         }
         else if (offset + limit > count)
         {
            // it is possible to select only count-offset nodes from temporary table #tempA
            newLimit = count - offset;
         }

         selectLimitNodesInTemporaryTable.setMaxRows(newLimit);
         selectLimitNodesInTemporaryTable.execute();

         tempTableBCreated = true;

         findNodesAndProperties.setString(1, containerName);

         return findNodesAndProperties.executeQuery();
      }
      finally
      {
         if (tempTableACreated)
         {
            try
            {
               deleteTemporaryTableA.execute();
            }
            catch (SQLException e)
            {
               LOG.warn("Can not delete temporary table " + tempTableAName);
            }
         }

         if (tempTableBCreated)
         {
            try
            {
               deleteTemporaryTableB.execute();
            }
            catch (SQLException e)
            {
               LOG.warn("Can not delete temporary table " + tempTableBName);
            }
         }

         // close prepared statement since we always create new
         if (selectLimitOffsetNodesIntoTemporaryTable != null)
         {
            selectLimitOffsetNodesIntoTemporaryTable.close();
         }

         if (selectLimitNodesInTemporaryTable != null)
         {
            selectLimitNodesInTemporaryTable.close();
         }

         if (deleteTemporaryTableA != null)
         {
            deleteTemporaryTableA.close();
         }

         if (deleteTemporaryTableB != null)
         {
            deleteTemporaryTableB.close();
         }

         if (countNodesInTemporaryTable != null)
         {
            countNodesInTemporaryTable.close();
         }
      }
   }
}
