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

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

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
 * @version $Id$
 */
public class GenericCQConnectionFactory extends GenericConnectionFactory
{

   /**
    * GenericConnectionFactory constructor.
    */
   public GenericCQConnectionFactory(DataSource dataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dataSource, containerConfig);
   }

   /**
    * GenericConnectionFactory constructor.
    */
   public GenericCQConnectionFactory(JDBCDataContainerConfig containerConfig) throws RepositoryException
   {

      this(null, containerConfig);

      try
      {
         ClassLoading.forName(this.containerConfig.dbDriver, this).newInstance();
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
   @Override
   public WorkspaceStorageConnection openConnection() throws RepositoryException
   {
      return openConnection(true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {

         if (this.containerConfig.dbStructureType.isSimpleTable())
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
   @Override
   public Connection getJdbcConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         Connection conn =
            dbDataSource != null ? dbDataSource.getConnection() : (this.containerConfig.dbUserName != null
               ? DriverManager.getConnection(this.containerConfig.dbUrl, this.containerConfig.dbUserName,
                  this.containerConfig.dbPassword) : DriverManager.getConnection(this.containerConfig.dbUrl));

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
   @Override
   public Connection getJdbcConnection() throws RepositoryException
   {
      return getJdbcConnection(false);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isReindexingSupport()
   {
      return true;
   }
}
