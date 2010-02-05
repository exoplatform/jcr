/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache.jdbc;

import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: LockPersistentDataManager.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class LockJDBCContainer
{
   private final Log log = ExoLogger.getLogger(LockJDBCContainer.class);

   private DataSource dataSource;

   private String wsName;

   /**
    * @param dataSourceName DataSource name
    * @param tableName Name of DB table
    * @throws RepositoryException 
    */
   public LockJDBCContainer(String dataSourceName, String wsName) throws RepositoryException
   {
      // TODO : rework exception handling
      this.wsName = wsName;

      // try to resolve DataSource
      try
      {
         dataSource = (DataSource)new InitialContext().lookup(dataSourceName);
         if (dataSource != null)
         {
            // initialize DB table if needed
            Connection jdbcConn = null;
            try
            {
               log.info("Creating LockManager DB tables.");
               jdbcConn = dataSource.getConnection();
               // if table not exists, create it  
               // connection is closed by DB initializer
               initDatabase(dataSourceName, jdbcConn, DialectDetecter.detect(jdbcConn.getMetaData()));
            }
            catch (SQLException e)
            {
               throw new RepositoryException(e);
            }
            catch (IOException e)
            {
               throw new RepositoryException(e);
            }
            finally
            {
               if (jdbcConn != null)
               {
                  try
                  {
                     jdbcConn.close();
                  }
                  catch (SQLException e)
                  {
                     log.error("Error of connection close", e);
                  }
               }
            }
         }
         else
         {
            throw new RepositoryException("Datasource '" + dataSourceName + "' is not bound in this context.");
         }
      }
      catch (Exception e)
      {
         throw new RepositoryException(e);
      }

   }

   public LockJDBCConnection openConnection() throws RepositoryException
   {
      try
      {
         return new LockJDBCConnection(getJDBCConnection(), wsName);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Returns connection to database
    * @return 
    * 
    * @return connection to database
    * @throws RepositoryException 
    * @throws RepositoryException
    */
   private Connection getJDBCConnection() throws LockException
   {
      try
      {
         //TODO make connection as in GenericConnectionFactory
         return dataSource.getConnection();

      }
      catch (SQLException e)
      {
         String err =
            "Error of JDBC connection open. SQLException: " + e.getMessage() + ", SQLState: " + e.getSQLState()
               + ", VendorError: " + e.getErrorCode();
         throw new LockException(err, e);
      }
   }

   /**
    * Creates table in DB if not present
    * 
    * @param dialect
    * @throws IOException 
    * @throws DBInitializerException 
    */
   protected void initDatabase(String dataSource, Connection jdbcConn, String dialect) throws IOException,
      DBInitializerException
   {
      DBInitializer dbInitializer = new DBInitializer(dataSource, jdbcConn, "/conf/storage/jcr-lock-jdbc.sql");

      // init DB
      dbInitializer.init();
   }

}