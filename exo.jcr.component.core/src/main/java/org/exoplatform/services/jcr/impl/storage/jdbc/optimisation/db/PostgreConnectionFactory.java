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

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: PostgreConnectionFactory.java 111 4.05.2011 serg $
 */
public class PostgreConnectionFactory extends GenericCQConnectionFactory
{
   /**
    * PostgreConnectionFactory constructor.
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
    * @throws RepositoryException
    *           if error eccurs
    */
   public PostgreConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner) throws RepositoryException
   {

      super(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);
   }

   /**
    * PostgreConnectionFactory  constructor.
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
   public PostgreConnectionFactory(DataSource dbDataSource, String containerName, boolean multiDb,
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
            return new PostgreMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         return new PostgreSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
            valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

}
