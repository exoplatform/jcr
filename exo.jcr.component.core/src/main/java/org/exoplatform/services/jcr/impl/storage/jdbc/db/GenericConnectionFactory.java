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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS
 * 
 * 15.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: GenericConnectionFactory.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class GenericConnectionFactory implements WorkspaceStorageConnectionFactory
{
   protected final Log log = ExoLogger.getLogger("exo.jcr.component.core.GenericConnectionFactory");

   protected final DataSource dbDataSource;

   protected final String dbDriver;

   protected final String dbUrl;

   protected final String dbUserName;

   protected final String dbPassword;

   protected final String containerName;

   protected final boolean multiDb;

   protected final ValueStoragePluginProvider valueStorageProvider;

   protected final int maxBufferSize;

   protected final File swapDirectory;

   protected final FileCleaner swapCleaner;

   /**
    * GenericConnectionFactory constructor.
    * 
    * @param dataSource
    *          - DataSource
    * @param dbDriver
    *          - JDBC Driver
    * @param dbUrl
    *          - JDBC URL
    * @param dbUserName
    *          - database username
    * @param dbPassword
    *          - database user password
    * @param containerName
    *          - Container name (see configuration)
    * @param multiDb
    *          - multidatabase state flag
    * @param valueStorageProvider
    *          - external Value Storages provider
    * @param maxBufferSize
    *          - Maximum buffer size (see configuration)
    * @param swapDirectory
    *          - Swap directory (see configuration)
    * @param swapCleaner
    *          - Swap cleaner (internal FileCleaner).
    */
   protected GenericConnectionFactory(DataSource dataSource, String dbDriver, String dbUrl, String dbUserName,
      String dbPassword, String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider,
      int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
   {

      this.containerName = containerName;
      this.multiDb = multiDb;
      this.valueStorageProvider = valueStorageProvider;
      this.maxBufferSize = maxBufferSize;
      this.swapDirectory = swapDirectory;
      this.swapCleaner = swapCleaner;

      this.dbDataSource = dataSource;
      this.dbDriver = dbDriver;
      this.dbUrl = dbUrl;
      this.dbUserName = dbUserName;
      this.dbPassword = dbPassword;
   }

   /**
    * GenericConnectionFactory constructor.
    * 
    * @param dataSource
    *          - DataSource
    * @param containerName
    *          - Container name (see configuration)
    * @param multiDb
    *          - multidatabase state flag
    * @param valueStorageProvider
    *          - external Value Storages provider
    * @param maxBufferSize
    *          - Maximum buffer size (see configuration)
    * @param swapDirectory
    *          - Swap directory (see configuration)
    * @param swapCleaner
    *          - Swap cleaner (internal FileCleaner).
    */
   public GenericConnectionFactory(DataSource dataSource, String containerName, boolean multiDb,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
   {

      this(dataSource, null, null, null, null, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);
   }

   /**
    * GenericConnectionFactory constructor.
    * 
    * @param dbDriver
    *          - JDBC Driver
    * @param dbUrl
    *          - JDBC URL
    * @param dbUserName
    *          - database username
    * @param dbPassword
    *          - database user password
    * @param containerName
    *          - Container name (see configuration)
    * @param multiDb
    *          - multidatabase state flag
    * @param valueStorageProvider
    *          - external Value Storages provider
    * @param maxBufferSize
    *          - Maximum buffer size (see configuration)
    * @param swapDirectory
    *          - Swap directory (see configuration)
    * @param swapCleaner
    *          - Swap cleaner (internal FileCleaner).
    */
   public GenericConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner) throws RepositoryException
   {

      this(null, dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);

      try
      {
         Class.forName(dbDriver).newInstance();
      }
      catch (InstantiationException e)
      {
         throw new RepositoryException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new RepositoryException(e);
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection() throws RepositoryException
   {
      return openConnection(false);
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {

         if (multiDb)
         {
            return new MultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         return new SingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName, valueStorageProvider,
            maxBufferSize, swapDirectory, swapCleaner);

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Connection getJdbcConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         Connection conn =
            dbDataSource != null ? dbDataSource.getConnection() : (dbUserName != null ? DriverManager.getConnection(
               dbUrl, dbUserName, dbPassword) : DriverManager.getConnection(dbUrl));
         if (readOnly)
         {
            // set this feature only if it asked
            conn.setReadOnly(readOnly);
         }

         return conn;
      }
      catch (SQLException e)
      {
         String err =
            "Error of JDBC connection open. SQLException: " + e.getMessage() + ", SQLState: " + e.getSQLState()
               + ", VendorError: " + e.getErrorCode();
         throw new RepositoryException(err, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Connection getJdbcConnection() throws RepositoryException
   {
      return getJdbcConnection(false);
   }

   /**
    * Indicates if the component supports extracting data from storage using paging.
    * @return <code>true</code> if it is supported, <code>false</code> otherwise.
    *
    */   
   public boolean isReindexingSupported()
   {
      return false;
   }
   
   /**
    * Indicates whether the id of the last item is needed for paging
    * @return <code>true</code> if the id is needed, <code>false</code> otherwise.
    */
   public boolean isIDNeededForPaging()
   {
      return true;
   }

   /**
    * Indicates whether the database allows to set an offset to the query
    * @return <code>true</code> if it is possible to set an offset, <code>false</code> otherwise.
    */
   public boolean isOffsetSupported()
   {
      return true;
   }
}
