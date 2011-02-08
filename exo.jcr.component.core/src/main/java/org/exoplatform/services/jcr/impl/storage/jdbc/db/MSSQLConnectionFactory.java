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

import java.io.File;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02  2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: MSSQLConnectionFactory.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class MSSQLConnectionFactory extends GenericConnectionFactory
{

   /**
    * MSSQLConnectionFactory constructor.
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
   public MSSQLConnectionFactory(String dbDriver, String dbUrl, String dbUserName, String dbPassword,
      String containerName, boolean multiDb, ValueStoragePluginProvider valueStorageProvider, int maxBufferSize,
      File swapDirectory, FileCleaner swapCleaner) throws RepositoryException
   {
      super(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb, valueStorageProvider, maxBufferSize,
         swapDirectory, swapCleaner);
   }

   /**
    * MSSQLConnectionFactory  constructor.
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
   public MSSQLConnectionFactory(DataSource dbDataSource, String containerName, boolean multiDb,
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
            return new MSSQLMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         return new MSSQLSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerName,
            valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }
}
