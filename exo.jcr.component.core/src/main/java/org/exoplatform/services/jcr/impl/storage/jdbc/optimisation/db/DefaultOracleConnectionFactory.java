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

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 19 mars 2010  
 */
public class DefaultOracleConnectionFactory extends GenericCQConnectionFactory
{

   protected boolean forceQueryHints;

   /**
    * DefaultOracleConnectionFactory constructor.
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
    * @param forceQueryHints
    *          - use Oracle queries with query hints
    */
   public DefaultOracleConnectionFactory(DataSource dataSource, String containerName, boolean multiDb,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner,
      boolean forceQueryHints)
   {
      super(dataSource, containerName, multiDb, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
      this.forceQueryHints = forceQueryHints;
   }

   /**
    * DefaultOracleConnectionFactory constructor.
    * 
    *@param dataSource
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
    * @param forceQueryHints
    *          - use Oracle queries with query hints
    * @throws RepositoryException
    *           if error eccurs
    */
   public DefaultOracleConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner, boolean forceQueryHints) throws RepositoryException
   {

      super(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);
      this.forceQueryHints = forceQueryHints;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         if (forceQueryHints)
         {
            if (multiDb)
            {
               return new OracleMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
            }

            return new OracleSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }
         else
         {
            // use common CQ queries, since Oracle[Multi/Single]DbJDBCConnection contains only queries with hints
            return super.openConnection(readOnly);
         }

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
}
