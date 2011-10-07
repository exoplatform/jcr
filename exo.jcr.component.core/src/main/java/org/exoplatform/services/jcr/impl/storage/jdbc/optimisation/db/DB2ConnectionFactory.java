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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DB2ConnectionFactory.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DB2ConnectionFactory extends GenericCQConnectionFactory
{

   /**
    * DB2ConnectionFactory constructor.
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
    * @throws RepositoryException
    *           if error eccurs
    */
   public DB2ConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner) throws RepositoryException
   {
      super(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);
   }

   /**
    * DB2ConnectionFactory  constructor.
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
   public DB2ConnectionFactory(DataSource dbDataSource, String containerName, boolean multiDb,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
   {
      super(dbDataSource, containerName, multiDb, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         if (multiDb)
         {
            return new DB2MultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         return new DB2SingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
            valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isIDNeededForPaging()
   {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isReindexingSupport()
   {
      Connection con = null;
      try
      {
         con = getJdbcConnection();
         DatabaseMetaData metaData = con.getMetaData();
         if (metaData.getDatabaseMajorVersion() > 9)
         {
            return true;
         }
         else if (metaData.getDatabaseMajorVersion() == 9 && metaData.getDatabaseMinorVersion() > 7)
         {
            return true;
         }
         else if (metaData.getDatabaseMajorVersion() == 9 && metaData.getDatabaseMinorVersion() == 7)
         {
            // returned string like 'SQL09074'
            char maintenanceVersion =
               metaData.getDatabaseProductVersion().charAt(metaData.getDatabaseProductVersion().length() - 1);
            if (new Integer(maintenanceVersion) >= 2)
            {
               return true;
            }
         }
      }
      catch (SQLException e)
      {
         log.error("Error checking product version.", e);
      }
      catch (RepositoryException e)
      {
         log.error("Error checking product version.", e);
      }
      finally
      {
         if (con != null)
         {
            try
            {
               con.close();
            }
            catch (SQLException e)
            {
               // ignore me
            }
         }
      }
      
      log.debug("The version of DB2 is prior to 9.7.2, so the old indexing mechanism will be used");
      return false;
   }   
}
