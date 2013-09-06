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

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
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
   protected static final Log log = ExoLogger.getLogger("exo.jcr.component.core.GenericConnectionFactory");

   protected final DataSource dbDataSource;

   protected final JDBCDataContainerConfig containerConfig;

   /**
    * GenericConnectionFactory constructor.
    */
   public GenericConnectionFactory(DataSource dataSource, JDBCDataContainerConfig containerConfig)
   {
      this.containerConfig = containerConfig;
      this.dbDataSource = dataSource;
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

         if (this.containerConfig.dbStructureType.isMultiDatabase())
         {
            return new MultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
         }

         return new SingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);

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
         Connection conn = dbDataSource.getConnection();

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
