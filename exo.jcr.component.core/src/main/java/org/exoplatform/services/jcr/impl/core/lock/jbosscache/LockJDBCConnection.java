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
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

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

   protected String REFRESH_LOCK_DATA;

   protected String GET_LOCKED_NODES;

   private PreparedStatement insertLockData;

   private PreparedStatement removeLockData;

   private PreparedStatement refreshLockData;

   private PreparedStatement getLockedNodes;

   private Connection dbConnection;

   public LockJDBCConnection(Connection dbConnection, String wsName) throws SQLException
   {

      this.dbConnection = dbConnection;

      if (dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }

      prepareQueries(wsName);
   }

   protected void prepareQueries(String wsName) throws SQLException
   {
      // Table structure
      // CREATE TABLE JCR_LOCKS(
      //      WS_NAME VARCHAR(96) NOT NULL,
      //      NODE_ID VARCHAR(96) NOT NULL,
      //      TOKEN_HASH VARCHAR(32) NOT NULL,
      //      OWNER VARCHAR(96) NOT NULL,
      //      IS_SESSIONSCOPED CHAR NOT NULL,
      //      IS_DEEP CHAR NOT NULL,
      //      BIRTHDAY LONG NOT NULL,
      //      TIMEOUT LONG NOT NULL
      // )

      ADD_LOCK_DATA =
         "insert into JCR_LOCKS"
            + "(WS_NAME, NODE_ID, TOKEN_HASH, OWNER, IS_SESSIONSCOPED, IS_DEEP, BIRTHDAY, TIMEOUT) VALUES( " + wsName
            + " ,?,?,?,?,?,?,?)";

      REMOVE_LOCK_DATA = "delete from JCR_LOCKS where WS_NAME=" + wsName + " and NODE_ID=?";

      REFRESH_LOCK_DATA =
      // TODO check list of updated columns
         "update JCR_LOCKS set OWNER=?, IS_SESSIONSCOPED=?, IS_DEEP=?, BIRTHDAY=?, TIMEOUT=? where NODE_ID=?";

      GET_LOCKED_NODES = "select NODE_ID from JCR_LOCKS where WS_NAME=" + wsName;

   }

   public int addLockData(LockData data) throws LockException
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
         throw new LockException(e);
      }
   }

   public int removeLockData(String nodeID) throws LockException
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
         throw new LockException(e);
      }
   }

   /**
    * Refreshes given lockData
    */
   public int refreshLockData(LockData data) throws LockException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (refreshLockData == null)
            refreshLockData = dbConnection.prepareStatement(REFRESH_LOCK_DATA);
         else
            refreshLockData.clearParameters();

         //update JCR_LOCKS set OWNER=?, IS_SESSIONSCOPED=?, IS_DEEP=?, BIRTHDAY=?, TIMEOUT=? where NODE_ID=?;         

         refreshLockData.setString(1, data.getOwner());
         refreshLockData.setBoolean(2, data.isSessionScoped());
         refreshLockData.setBoolean(3, data.isDeep());
         refreshLockData.setLong(4, data.getBirthDay());
         refreshLockData.setLong(5, data.getTimeOut());
         refreshLockData.setString(6, data.getNodeIdentifier());

         return removeLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new LockException(e);
      }
   }

   /**
    * Returns set of locked nodes identifiers
    */
   public Set<String> getLockedNodes() throws LockException
   {
      return null;
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
