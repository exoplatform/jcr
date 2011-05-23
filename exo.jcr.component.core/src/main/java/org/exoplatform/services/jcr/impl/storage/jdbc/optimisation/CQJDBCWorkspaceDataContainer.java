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
import org.exoplatform.services.jcr.impl.storage.jdbc.init.IngresSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.OracleDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.PgSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.StorageDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.DB2ConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.DefaultOracleConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.GenericCQConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.HSQLDBConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.MSSQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.MySQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.OracleConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.SybaseConnectionFactory;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.jdbc.DataSourceProvider;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picocontainer.Startable;

import java.io.IOException;

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
      FileCleanerHolder fileCleanerHolder, DataSourceProvider dsProvider) throws RepositoryConfigurationException, NamingException,
      RepositoryException, IOException
   {
      super(wsConfig, repConfig, contextInit, valueStorageProvider, fileCleanerHolder, dsProvider);
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

      StorageDBInitializer dbInitilizer = null;
      String sqlPath = null;
      if (dbDialect == DBConstants.DB_DIALECT_ORACLEOCI)
      {
         LOG.warn(DBConstants.DB_DIALECT_ORACLEOCI + " dialect is experimental!");
         // sample of connection factory customization
         if (dbSourceName != null)
         {
            this.connFactory =
               new DefaultOracleConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner, useQueryHints);
         }
         else
            this.connFactory =
               new OracleConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner, useQueryHints);

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ora.sql";

         // a particular db initializer may be configured here too
         dbInitilizer = new OracleDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_ORACLE)
      {

         if (dbSourceName != null)
         {
            this.connFactory =
               new DefaultOracleConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner, useQueryHints);
         }
         else
            this.connFactory =
               new DefaultOracleConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner, useQueryHints);
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ora.sql";
         dbInitilizer = new OracleDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);

      }
      else if (dbDialect == DBConstants.DB_DIALECT_PGSQL)
      {
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.pgsql.sql";
         dbInitilizer = new PgSQLDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MYSQL)
      {
         // [PN] 28.06.07
         if (dbSourceName != null)
         {
            this.connFactory =
               new MySQLConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
            this.connFactory =
               new MySQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MYSQL_UTF8)
      {
         // [PN] 13.07.08
         if (dbSourceName != null)
         {
            this.connFactory =
               new MySQLConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
            this.connFactory =
               new MySQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-utf8.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MSSQL)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new MSSQLConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new MSSQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mssql.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DERBY)
      {
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.derby.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DB2)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new DB2ConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new DB2ConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DB2V8)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new DB2ConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new DB2ConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2v8.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_SYBASE)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new SybaseConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new SybaseConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sybase.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_INGRES)
      {
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ingres.sql";
         // using Postgres initializer
         dbInitilizer =
            new IngresSQLDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_HSQLDB)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new HSQLDBConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
            this.connFactory =
               new HSQLDBConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else
      {
         // generic, DB_HSQLDB
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }

      // database type
      try
      {
         dbInitilizer.init();
      }
      catch (DBInitializerException e)
      {
         LOG.error("Error of init db " + e, e);
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
      // by default
      if (dbSourceName != null)
      {
         return new GenericCQConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
            swapDirectory, swapCleaner);
      }

      return new GenericCQConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
         valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   } 
}
