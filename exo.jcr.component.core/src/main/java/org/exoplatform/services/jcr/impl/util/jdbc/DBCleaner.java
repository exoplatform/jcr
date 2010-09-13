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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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

   protected String REMOVE_ITEMS;

   protected String REMOVE_VALUES;

   protected String REMOVE_REFERENCES;

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
         connection.commit();
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
      REMOVE_ITEMS = "delete from JCR_SITEM where CONTAINER_NAME=?";

      REMOVE_VALUES =
         "delete from JCR_SVALUE V where exists "
            + "( select * from JCR_SITEM I where I.ID=V.PROPERTY_ID and I.CONTAINER_NAME=? )";

      //TODO R.PROPERTY_ID or R.NODE_ID?
      REMOVE_REFERENCES =
         "delete from JCR_SREF R where exists "
            + "( select * from JCR_SITEM I where I.ID=R.PROPERTY_ID and I.CONTAINER_NAME=? )";

      // for multi db support
      //TODO do we need remove indexes? 
      //different databases may be configured to use different indexes
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
      PreparedStatement statements = null;
      try
      {
         statements = connection.prepareStatement(REMOVE_REFERENCES);
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

      try
      {
         statements = connection.prepareStatement(REMOVE_VALUES);
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

      try
      {
         statements = connection.prepareStatement(REMOVE_ITEMS);
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
}
