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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.util.jdbc;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The goal of this class is remove workspace data from database.
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleaner.java 111 2008-11-11 11:11:11Z serg $
 */
public class DBCleaner
{

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleaner");

   protected String REMOVE_PROPERTIES;

   protected String REMOVE_ROOT;

   protected String REMOVE_ITEMS;

   protected String REMOVE_VALUES;

   protected String REMOVE_REFERENCES;

   protected String GET_CHILD_IDS;

   protected String DROP_JCR_MITEM_TABLE;

   protected String DROP_JCR_MVALUE_TABLE;

   protected String DROP_MREF_TABLE;

   private final Connection connection;

   private final String containerName;

   private final boolean isMultiDB;

   /**
    * Constructor.
    * 
    * @param containerName - workspace name
    * @param connection - SQL conneciton
    */
   public DBCleaner(Connection connection, String containerName, boolean isMulti)
   {
      this.connection = connection;
      this.containerName = containerName;
      this.isMultiDB = isMulti;
      prepareQueries();
   }

   /**
    * Remove workspace data from database.
    * <ul>
    * <li>If workspace uses multiDB data source - tables associated with this workspace
    * will be dropped.
    * <li>If workspace uses singleDB data source - all records of this workspace will
    * be removed.
    * </ul> 
    *  
    * <p>Connection used by this method will be closed at final.
    * 
    * @throws DBCleanerException - if exception during data cleanup occures.
    */
   public void cleanWorkspace() throws DBCleanerException
   {
      try
      {
         //connection.setAutoCommit(false);
         // check is multi db
         if (isMultiDB)
         {
            //remove table
            dropWorkspace();
         }
         else
         {
            // clean up all record of this container
            removeWorkspaceRecords();
         }
         //connection.commit();
      }
      catch (SQLException e)
      {

         // TODO do we need rollback here?
         try
         {
            connection.rollback();
         }
         catch (SQLException rollbackException)
         {
            LOG.error("Can not rollback changes after exception " + e.getMessage(), rollbackException);
         }
         throw new DBCleanerException(e.getMessage(), e);
      }
      finally
      {
         try
         {
            connection.close();
         }
         catch (SQLException e)
         {
            LOG.error("Error of a connection closing. " + e, e);
         }
      }
   }

   protected void prepareQueries()
   {
      //for single db support

      REMOVE_PROPERTIES = "delete from JCR_SITEM where I_CLASS=2 and CONTAINER_NAME=?";

      GET_CHILD_IDS =
         "select ID from JCR_SITEM where CONTAINER_NAME=? and ID not in(select PARENT_ID from JCR_SITEM where CONTAINER_NAME=?)";

      REMOVE_ITEMS =
      //   "delete from JCR_SITEM where CONTAINER_NAME=?";
         "delete from JCR_SITEM where ID in( ? )";

      //REMOVE_ROOT = "delete from JCR_SITEM where CONTAINER_NAME=? and ID=?";

      REMOVE_VALUES =
         "delete from JCR_SVALUE where exists"
            + "(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?)";

      REMOVE_REFERENCES =
         "delete from JCR_SREF where exists"
            + "(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME=?)";

      // for multi db support
      DROP_JCR_MITEM_TABLE = "DROP TABLE JCR_MITEM";
      DROP_JCR_MVALUE_TABLE = "DROP TABLE JCR_MVALUE";
      DROP_MREF_TABLE = "DROP TABLE JCR_MREF";
   }

   protected void dropWorkspace() throws SQLException
   {
      final Statement statement = connection.createStatement();
      connection.setAutoCommit(false);

      try
      {
         // order of dropped tables is important
         statement.executeUpdate(DROP_MREF_TABLE);
         statement.executeUpdate(DROP_JCR_MVALUE_TABLE);
         statement.executeUpdate(DROP_JCR_MITEM_TABLE);
      }
      finally
      {
         if (statement != null)
         {
            try
            {
               statement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   protected void removeWorkspaceRecords() throws SQLException
   {
      executeUpdate(connection, REMOVE_REFERENCES, containerName);
      executeUpdate(connection, REMOVE_VALUES, containerName);

      clearItems(connection, containerName);
   }

   protected void executeUpdate(Connection connection, String query, String containerName) throws SQLException
   {
      PreparedStatement statements = null;
      try
      {
         statements = connection.prepareStatement(query);
         statements.setString(1, containerName);
         statements.executeUpdate();
      }
      finally
      {
         if (statements != null)
         {
            statements.close();
            statements = null;
         }
      }
   }

   protected void clearItems(Connection connection, String containerName) throws SQLException
   {
      executeUpdate(connection, REMOVE_PROPERTIES, containerName);

      // Remove only child nodes in cycle, till all nodes will be removed.
      // Such algorithm used to avoid any constraint violation exception related to foreign key.

      PreparedStatement getChildItems = null;
      PreparedStatement removeItems = null;

      try
      {
         getChildItems = connection.prepareStatement(GET_CHILD_IDS);
         getChildItems.setString(1, containerName);
         getChildItems.setString(2, containerName);

         // TODO constant
         getChildItems.setMaxRows(100);

         //removeItems = connection.prepareStatement(REMOVE_ITEMS);

         do
         {
            ResultSet result = getChildItems.executeQuery();
            if (result.first())
            {
               StringBuilder childListBuilder = new StringBuilder("'" + result.getString(1) + "'");
               while (result.next())
               {
                  childListBuilder.append(" , '" + result.getString(1) + "'");
               }

               // now remove nodes;
               String q = REMOVE_ITEMS.replace("?", childListBuilder.toString());
               removeItems = connection.prepareStatement(q);
               //removeItems.se.setString(1, childListBuilder.toString());
               int res = removeItems.executeUpdate();
            }
            else
            {
               break;
            }

         }
         while (true);
      }
      finally
      {
         if (getChildItems != null)
         {
            getChildItems.close();
            getChildItems = null;
         }
         if (removeItems != null)
         {
            removeItems.close();
            removeItems = null;
         }
      }
   }
}
