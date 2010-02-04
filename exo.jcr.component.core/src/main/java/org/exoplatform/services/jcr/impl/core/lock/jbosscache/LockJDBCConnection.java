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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: LockJDBCConnection.java 111 2008-11-11 11:11:11Z serg $
 */
public class LockJDBCConnection
{

   private final Log LOG = ExoLogger.getLogger(LockPersistentDataManager.class);

   protected String ADD_LOCK_DATA;

   protected String REMOVE_LOCK_DATA;

   private PreparedStatement insertLockData;

   private PreparedStatement removeLockData;

   private Connection dbConnection;

   public LockJDBCConnection(Connection dbConnection, String tableName) throws SQLException
   {

      this.dbConnection = dbConnection;

      if (dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }

      prepareQueries(tableName);
   }

   protected void prepareQueries(String tableName) throws SQLException
   {
      // Table structure
      // CREATE TABLE ${table.name}(
      //      NODE_ID VARCHAR(96) NOT NULL,
      //      TOKEN_HASH VARCHAR(32) NOT NULL,
      //      OWNER VARCHAR(96) NOT NULL,
      //      IS_SESSIONSCOPED CHAR NOT NULL,
      //      IS_DEEP CHAR NOT NULL,
      //      BIRTHDAY LONG NOT NULL,
      //      TIMEOUT LONG NOT NULL
      // )

      ADD_LOCK_DATA =
         "insert into " + tableName
            + "(NODE_ID, TOKEN_HASH, OWNER, IS_SESSIONSCOPED, IS_DEEP, BIRTHDAY, TIMEOUT) VALUES(?,?,?,?,?,?,?)";

      REMOVE_LOCK_DATA = "delete from " + tableName + " where NODE_ID=?";

   }

   public int addLockData(LockData data) throws RepositoryException
   {

      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (insertLockData == null)
            insertLockData = dbConnection.prepareStatement(ADD_LOCK_DATA);
         else
            insertLockData.clearParameters();

         insertLockData.setString(1, data.getNodeIdentifier());
         insertLockData.setString(2, data.getTokenHash());
         insertLockData.setString(3, data.getOwner());
         insertLockData.setBoolean(4, data.isSessionScoped());
         insertLockData.setBoolean(5, data.isDeep());
         insertLockData.setLong(6, data.getBirthDay());
         insertLockData.setLong(7, data.getTimeOut());

         return insertLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   public int removeLockData(String nodeID) throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (removeLockData == null)
            removeLockData = dbConnection.prepareStatement(ADD_LOCK_DATA);
         else
            removeLockData.clearParameters();

         removeLockData.setString(1, nodeID);

         return removeLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isOpened()
   {
      try
      {
         return !dbConnection.isClosed();
      }
      catch (SQLException e)
      {
         LOG.error(e.getMessage(), e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void close() throws IllegalStateException, RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }

      try
      {
         dbConnection.close();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void commit() throws IllegalStateException, RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }

      try
      {
         if (!dbConnection.isReadOnly())
         {
            dbConnection.commit();
         }

         dbConnection.close();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

}
