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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.DB2DBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.H2DBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.HSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.MSSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.MysqlDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.IngresSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.OracleDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.PgSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.SybaseDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.DB2ConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.DefaultOracleConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.GenericCQConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.H2ConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.HSQLDBConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.MSSQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.MySQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.PostgreConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.PostgreSCSConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.SybaseConnectionFactory;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.jdbc.DataSourceProvider;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picocontainer.Startable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id:GenericWorkspaceDataContainer.java 13433 2007-03-15 16:07:23Z peterit $
 */
public class CQJDBCWorkspaceDataContainer extends JDBCWorkspaceDataContainer implements Startable
{

   /**
    * Constructor with value storage plugins.
    * 
    * @param wsConfig
    *          Workspace configuration
    * @param valueStrorageProvider
    *          External Value Storages provider
    * @param dsProvider
    *          The data source provider
    * @throws RepositoryConfigurationException
    *           if Repository configuration is wrong
    * @throws NamingException
    *           if JNDI exception (on DataSource lookup)
    */
   public CQJDBCWorkspaceDataContainer(WorkspaceEntry wsConfig, RepositoryEntry repConfig,
      InitialContextInitializer contextInit, ValueStoragePluginProvider valueStorageProvider,
      DataSourceProvider dsProvider, FileCleanerHolder fileCleanerHolder) throws RepositoryConfigurationException,
      NamingException, RepositoryException, IOException
   {
      super(wsConfig, repConfig, contextInit, valueStorageProvider, dsProvider, fileCleanerHolder);
   }

   /**
    * Init storage database.
    * 
    * @throws NamingException
    *           on JNDI error
    * @throws RepositoryException
    *           on storage error
    * @throws IOException
    *           on I/O error
    */
   @Override
   protected void initDatabase() throws NamingException, RepositoryException, IOException
   {
      DBInitializer dbInitializer = null;
      if (containerConfig.dbDialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         LOG.warn(DBConstants.DB_DIALECT_ORACLEOCI + " dialect is experimental!");
         this.connFactory = new DefaultOracleConnectionFactory(getDataSource(), containerConfig);
         dbInitializer =
            new OracleDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_ORACLE))
      {
         this.connFactory = new DefaultOracleConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new OracleDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.equals(DBConstants.DB_DIALECT_PGSQL))
      {
         this.connFactory = new PostgreConnectionFactory(getDataSource(), containerConfig);
         dbInitializer =
            new PgSQLDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.equals(DBConstants.DB_DIALECT_PGSQL_SCS))
      {
         this.connFactory = new PostgreSCSConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new PgSQLDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_MYSQL))
      {
         if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_MYSQL_MYISAM))
         {
            LOG.warn("MyISAM is not supported due to its lack of transaction support and integrity check, so use it only"
               + " if you don't expect any support and performances in read accesses are more important than the consistency"
               + " in your use-case. This dialect is only dedicated to the community.");
         }

         this.connFactory = new MySQLConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new MysqlDBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_MSSQL))
      {
         this.connFactory = new MSSQLConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new MSSQLDBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_DERBY))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_DB2))
      {
         this.connFactory = new DB2ConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new DB2DBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
      {
         this.connFactory = new SybaseConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new SybaseDBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_INGRES))
      {
         this.connFactory = defaultConnectionFactory();
         // using Postgres initializer
         dbInitializer =
            new IngresSQLDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_HSQLDB))
      {
         this.connFactory = new HSQLDBConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new HSQLDBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_H2))
      {
         this.connFactory = new H2ConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = new H2DBInitializer(this.connFactory.getJdbcConnection(),containerConfig);
      }
      else
      {
         // generic, DB_HSQLDB
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }

      validateBatchingUpdate();

      // database type
      try
      {
         dbInitializer.init();
      }
      catch (DBInitializerException e)
      {
         LOG.error("Error of init db " + e, e);
      }
   }

   private void validateBatchingUpdate()
   {
      Connection con = null;

      if (containerConfig.batchSize > 1)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Batching update is enabled with batch size " + containerConfig.batchSize);
         }

         try
         {
            if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
            {
               containerConfig.batchSize = -1;
               LOG.info("Batching update is disabled on Sybase DB.");
               return;
            }
            con = getConnectionFactory().getJdbcConnection();
            if (!con.getMetaData().supportsBatchUpdates())
            {
               containerConfig.batchSize = -1;
               LOG.info("Batching update is disabled since DB does not support it.");
            }
         }
         catch (SQLException e)
         {
            LOG.error("Error checking batching update support.", e);
         }
         catch (RepositoryException e)
         {
            LOG.error("Error checking batching update support.", e);
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
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
            }
         }
      }
   }

   /**
    * Prepare default connection factory.
    * 
    * @return GenericConnectionFactory
    * @throws NamingException
    *           on JNDI error
    * @throws RepositoryException
    *           on Storage error
    */
   @Override
   protected GenericConnectionFactory defaultConnectionFactory() throws NamingException, RepositoryException
   {
      return new GenericCQConnectionFactory(getDataSource(), containerConfig);
   }
}
