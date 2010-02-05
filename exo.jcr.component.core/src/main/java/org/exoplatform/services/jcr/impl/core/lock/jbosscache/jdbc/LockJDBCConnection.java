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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache.jdbc;

import org.exoplatform.services.jcr.impl.core.lock.jbosscache.LockData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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

   private final Log LOG = ExoLogger.getLogger(LockJDBCContainer.class);

   // queries
   protected String ADD_LOCK_DATA;

   protected String REMOVE_LOCK_DATA;

   protected String REFRESH_LOCK_DATA;

   protected String GET_LOCKED_NODES;

   protected String GET_LOCK_DATA;

   // column names
   protected static String COLUMN_WS_NAME = "WS_NAME";

   protected static String COLUMN_NODE_ID = "NODE_ID";

   protected static String COLUMN_TOKEN_HASH = "TOKEN_HASH";

   protected static String COLUMN_OWNER = "OWNER";

   protected static String COLUMN_IS_SESSIONSCOPED = "IS_SESSIONSCOPED";

   protected static String COLUMN_IS_DEEP = "IS_DEEP";

   protected static String COLUMN_BIRTHDAY = "BIRTHDAY";

   protected static String COLUMN_TIMEOUT = "TIMEOUT";

   // prepared statements
   private PreparedStatement insertLockData;

   private PreparedStatement removeLockData;

   private PreparedStatement refreshLockData;

   private PreparedStatement getLockedNodes;

   private PreparedStatement getLockData;

   private Connection dbConnection;

   private String wsName;

   /**
    * Creates LockJDBCConnection instance based on given connection to 
    * database with specified workspace name.
    * 
    * @param dbConnection Connection to database.
    * @param wsName Current workspace's name.
    * @throws SQLException if database exception occurs.
    */
   public LockJDBCConnection(Connection dbConnection, String wsName) throws SQLException
   {
      this.dbConnection = dbConnection;

      if (dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }
      this.wsName = wsName;

      ADD_LOCK_DATA =
         "insert into JCR_LOCKS(WS_NAME, NODE_ID, TOKEN_HASH, OWNER, IS_SESSIONSCOPED, IS_DEEP, BIRTHDAY, TIMEOUT) VALUES(?,?,?,?,?,?,?,?)";

      REMOVE_LOCK_DATA = "delete from JCR_LOCKS where NODE_ID=? and WS_NAME=?";

      REFRESH_LOCK_DATA = "update JCR_LOCKS set BIRTHDAY=? where NODE_ID=? and WS_NAME=?";

      GET_LOCKED_NODES = "select NODE_ID from JCR_LOCKS where WS_NAME=?";

      GET_LOCK_DATA = "select * from JCR_LOCKS where NODE_ID=? and WS_NAME=?";
   }

   /**
    * Inserts new lock data into DB 
    * 
    * @param data
    * @return
    * @throws RepositoryException
    */
   public int addLockData(LockData data) throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (insertLockData == null)
         {
            insertLockData = dbConnection.prepareStatement(ADD_LOCK_DATA);
         }
         else
         {
            insertLockData.clearParameters();
         }
         insertLockData.setString(1, wsName);
         insertLockData.setString(2, data.getNodeIdentifier());
         insertLockData.setString(3, data.getTokenHash());
         insertLockData.setString(4, data.getOwner());
         insertLockData.setBoolean(5, data.isSessionScoped());
         insertLockData.setBoolean(6, data.isDeep());
         insertLockData.setLong(7, data.getBirthDay());
         insertLockData.setLong(8, data.getTimeOut());

         return insertLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Removes LockData for given node identifier from database
    * 
    * @param nodeID
    * @return
    * @throws RepositoryException
    */
   public int removeLockData(String nodeID) throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (removeLockData == null)
         {
            removeLockData = dbConnection.prepareStatement(REMOVE_LOCK_DATA);
         }
         else
         {
            removeLockData.clearParameters();
         }
         // REMOVE_LOCK_DATA = "delete from JCR_LOCKS where NODE_ID=? and WS_NAME=?";
         removeLockData.setString(1, nodeID);
         removeLockData.setString(2, wsName);

         return removeLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Refreshes given LockData (updates birthday column)
    * 
    * @param data
    * @return
    * @throws RepositoryException
    */
   public int refreshLockData(LockData data) throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (refreshLockData == null)
         {
            refreshLockData = dbConnection.prepareStatement(REFRESH_LOCK_DATA);
         }
         else
         {
            refreshLockData.clearParameters();
         }

         // REFRESH_LOCK_DATA = "update JCR_LOCKS set BIRTHDAY=? where NODE_ID=? and WS_NAME=?";
         refreshLockData.setLong(1, data.getBirthDay());
         refreshLockData.setString(2, data.getNodeIdentifier());
         refreshLockData.setString(3, wsName);

         return refreshLockData.executeUpdate();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Returns the set of locked nodes identifiers
    * 
    * @return
    * @throws RepositoryException
    */
   public Set<String> getLockedNodes() throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (getLockedNodes == null)
         {
            getLockedNodes = dbConnection.prepareStatement(GET_LOCKED_NODES);
         }
         else
         {
            getLockedNodes.clearParameters();
         }
         // GET_LOCKED_NODES = "select NODE_ID from JCR_LOCKS where WS_NAME=?";
         getLockedNodes.setString(1, wsName);
         // get result set
         ResultSet result = getLockedNodes.executeQuery();
         Set<String> identifiers = new HashSet<String>();
         // traverse result set
         while (result.next())
         {
            identifiers.add(new String(result.getString(COLUMN_NODE_ID)));
         }
         return identifiers;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Returns LockData for given node identifier from database
    * or null if not exists
    * 
    * @param identifier - locked node identifier
    * @return LockData
    * @throws RepositoryException
    */
   public LockData getLockData(String identifier) throws RepositoryException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is closed");
      }
      try
      {
         if (getLockData == null)
         {
            getLockData = dbConnection.prepareStatement(GET_LOCK_DATA);
         }
         else
         {
            getLockData.clearParameters();
         }
         // GET_LOCK_DATA = "select * from JCR_LOCKS where NODE_ID=? and WS_NAME=?";
         getLockData.setString(1, identifier);
         getLockData.setString(2, wsName);
         // get result set
         ResultSet result = getLockData.executeQuery();
         if (result.next())
         {
            return new LockData(result.getString(COLUMN_NODE_ID), result.getString(COLUMN_TOKEN_HASH), result
               .getBoolean(COLUMN_IS_DEEP), result.getBoolean(COLUMN_IS_SESSIONSCOPED), result.getString(COLUMN_OWNER),
               result.getLong(COLUMN_TIMEOUT), result.getLong(COLUMN_BIRTHDAY));
         }
         return null;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Check if connection is alive and opened 
    * @return
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
    * Closes database connection
    * 
    * @throws IllegalStateException
    * @throws RepositoryException
    */
   public final void close() throws IllegalStateException, RepositoryException
   {
      if (isOpened())
      {
         try
         {
            dbConnection.close();
         }
         catch (SQLException e)
         {
            throw new RepositoryException(e);
         }
      }
   }

   /**
    * Commits and closes database connection
    * 
    * @throws IllegalStateException
    * @throws RepositoryException
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
