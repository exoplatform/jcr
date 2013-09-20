/*
z * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.database.utils.ExceptionManagementHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.ComplexDataRestore;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBBackup;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.backup.rdbms.DirectoryRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.RestoreTableRule;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleaner;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.query.NodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.core.query.Reindexable;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.HSQLDBConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.MySQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.OracleConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.indexing.JdbcNodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.IngresSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.OracleDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.PgSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.StorageDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.statistics.StatisticsJDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.update.StorageUpdateManager;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.jdbc.DataSourceProvider;
import org.exoplatform.services.jdbc.impl.ManagedDataSource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picocontainer.Startable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id:GenericWorkspaceDataContainer.java 13433 2007-03-15 16:07:23Z peterit $
 */
public class JDBCWorkspaceDataContainer extends WorkspaceDataContainerBase implements Startable, Backupable,
   Reindexable
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainer");

   /**
    * Indicates if the statistics has to be enabled.
    */
   public static final boolean STATISTICS_ENABLED = Boolean.valueOf(PrivilegedSystemHelper
      .getProperty("JDBCWorkspaceDataContainer.statistics.enabled"));

   static
   {
      if (STATISTICS_ENABLED)
      {
         LOG.info("The statistics of the component JDBCWorkspaceDataContainer has been enabled");
      }
   }

   //configuration params

   public final static String SOURCE_NAME = "source-name";

   public final static String MULTIDB = "multi-db";

   public final static String SINGLEDB = "single-db";

   /**
    * Describe which type of RDBMS will be used (DB creation metadata etc.)
    */
   public final static String DB_DIALECT = "dialect";

   public final static String DB_DRIVER = "driverClassName";

   public final static String DB_URL = "url";

   public final static String DB_USERNAME = "username";

   public final static String DB_PASSWORD = "password";

   public final static String DB_FORCE_QUERY_HINTS = "force.query.hints";

   protected final String containerName;

   protected final String uniqueName;

   protected final String dbSourceName;

   protected final boolean multiDb;

   protected final String dbDriver;

   protected final String dbDialect;

   protected final String dbUrl;

   protected final String dbUserName;

   protected final String dbPassword;
   
   protected final DataSourceProvider dsProvider;

   protected final boolean isManaged;

   protected final ValueStoragePluginProvider valueStorageProvider;

   protected String storageVersion;

   protected boolean checkSNSNewConnection;

   protected int maxBufferSize;

   protected File swapDirectory;

   protected FileCleaner swapCleaner;

   protected GenericConnectionFactory connFactory;

   /**
    * Some DataBases supports query hints, that may improve query performance.
    * For default hints are enabled.
    */
   protected boolean useQueryHints;

   /**
    * Workspace configuration.
    */
   protected final WorkspaceEntry wsConfig;

   /**
    * Shared connection factory.
    * 
    * Issued to share JDBC connection between system and regular workspace in case of same database
    * used for storage.
    * 
    */
   class SharedConnectionFactory extends GenericConnectionFactory
   {

      /**
       * JDBC connection.
       */
      final private Connection connection;

      /**
       * SharedConnectionFactory constructor.
       * 
       * @param connection
       *          JDBC - connection
       * @param containerName
       *          - container name
       * @param multiDb
       *          - multidatabase status
       * @param valueStorageProvider
       *          - external Value Storages provider
       * @param maxBufferSize
       *          - Maximum buffer size (see configuration)
       * @param swapDirectory
       *          - Swap directory (see configuration)
       * @param swapCleaner
       *          - Swap cleaner (internal FileCleaner).
       */
      SharedConnectionFactory(Connection connection, String containerName, boolean multiDb,
         ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      {

         super(null, null, null, null, null, containerName, multiDb, valueStorageProvider, maxBufferSize,
            swapDirectory, swapCleaner);

         this.connection = connection;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Connection getJdbcConnection() throws RepositoryException
      {
         return connection;
      }
   }

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
   public JDBCWorkspaceDataContainer(WorkspaceEntry wsConfig, RepositoryEntry repConfig,
      InitialContextInitializer contextInit, ValueStoragePluginProvider valueStorageProvider,
      FileCleanerHolder fileCleanerHolder, DataSourceProvider dsProvider) throws RepositoryConfigurationException,
      NamingException, RepositoryException, IOException
   {
      checkIntegrity(wsConfig, repConfig);
      this.wsConfig = wsConfig;
      this.containerName = wsConfig.getName();
      this.uniqueName = wsConfig.getUniqueName();
      this.multiDb = Boolean.parseBoolean(wsConfig.getContainer().getParameterValue(MULTIDB));
      this.valueStorageProvider = valueStorageProvider;
      this.dsProvider = dsProvider;
      
      // ------------- Database config ------------------
      String pDbDialect = null;
      try
      {
         pDbDialect = DBInitializerHelper.validateDialect(wsConfig.getContainer().getParameterValue(DB_DIALECT));
      }
      catch (RepositoryConfigurationException e)
      {
         pDbDialect = DBConstants.DB_DIALECT_GENERIC;
      }

      String pDbDriver = null;
      String pDbUrl = null;
      String pDbUserName = null;
      String pDbPassword = null;
      try
      {
         pDbDriver = wsConfig.getContainer().getParameterValue(DB_DRIVER);

         // username/passwd may not pesent
         try
         {
            pDbUserName = wsConfig.getContainer().getParameterValue(DB_USERNAME);
            pDbPassword = wsConfig.getContainer().getParameterValue(DB_PASSWORD);
         }
         catch (RepositoryConfigurationException e)
         {
            pDbUserName = pDbPassword = null;
         }

         pDbUrl = wsConfig.getContainer().getParameterValue(DB_URL); // last here!
      }
      catch (RepositoryConfigurationException e)
      {
      }

      if (pDbUrl != null)
      {
         this.dbDriver = pDbDriver;
         this.dbUrl = pDbUrl;
         this.dbUserName = pDbUserName;
         this.dbPassword = pDbPassword;
         this.dbSourceName = null;
         // A managed data source is only possible when the source name is not null
         this.isManaged = false;
         LOG.info("Connect to JCR database as user '" + this.dbUserName + "'");

         if (DBConstants.DB_DIALECT_GENERIC.equalsIgnoreCase(pDbDialect)
            || DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(pDbDialect))
         {
            // try to detect via JDBC metadata
            Connection jdbcConn = null;
            try
            {
               jdbcConn =
                  dbUserName != null ? DriverManager.getConnection(dbUrl, dbUserName, dbPassword) : DriverManager
                     .getConnection(dbUrl);

               this.dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
            }
            catch (SQLException e)
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
                     throw new RepositoryException(e);
                  }
               }
            }
         }
         else
         {
            this.dbDialect = pDbDialect;
         }
      }
      else
      {
         this.dbDriver = null;
         this.dbUrl = null;
         this.dbUserName = null;
         this.dbPassword = null;

         String sn;
         try
         {
            sn = wsConfig.getContainer().getParameterValue(SOURCE_NAME);
         }
         catch (RepositoryConfigurationException e)
         {
            // for backward comp remove in rel.2.0
            sn = wsConfig.getContainer().getParameterValue("sourceName");
         }
         this.dbSourceName = sn;
         if (dsProvider == null)
         {
            throw new IllegalArgumentException(
               "Since a data source has been defined, the DataSourceProvider cannot be null, add it in your configuration.");
         }
         // the data source cannot be managed if there is no transaction manager
         this.isManaged = dsProvider.isManaged(dbSourceName);

         if (pDbDialect == DBConstants.DB_DIALECT_GENERIC)
         {
            // try to detect via JDBC metadata
            final DataSource ds = getDataSource();
            Connection jdbcConn = null;
            try
            {
               jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
               {
                  public Connection run() throws Exception
                  {
                     return ds.getConnection();
                  }
               });

               this.dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
            }
            catch (SQLException e)
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
                     throw new RepositoryException(e);
                  }
               }
            }
         }
         else
         {
            this.dbDialect = pDbDialect;
         }
      }
      LOG.info("Using a dialect '" + this.dbDialect + "'");

      // check is there DB_FORCE_QUERY_HINTS parameter - by default its enabled
      useQueryHints = wsConfig.getContainer().getParameterBoolean(DB_FORCE_QUERY_HINTS, true);

      try
      {
         this.checkSNSNewConnection = wsConfig.getContainer().getParameterBoolean(CHECK_SNS_NEW_CONNECTION);
      }
      catch (RepositoryConfigurationException e)
      {
         // don't use new connection by default
         this.checkSNSNewConnection = false;
      }

      // ------------- Values swap config ------------------
      try
      {
         this.maxBufferSize = wsConfig.getContainer().getParameterInteger(MAXBUFFERSIZE_PROP);
      }
      catch (RepositoryConfigurationException e)
      {
         this.maxBufferSize = DEF_MAXBUFFERSIZE;
      }

      try
      {
         String sdParam = wsConfig.getContainer().getParameterValue(SWAPDIR_PROP);
         this.swapDirectory = new File(sdParam);
      }
      catch (RepositoryConfigurationException e1)
      {
         this.swapDirectory = new File(DEF_SWAPDIR);
      }
      if (!PrivilegedFileHelper.exists(swapDirectory))
      {
         PrivilegedFileHelper.mkdirs(swapDirectory);
      }
      else
      {
         cleanupSwapDirectory();
      }

      this.swapCleaner = fileCleanerHolder.getFileCleaner();

      initDatabase();

      // enableStorageUpdate left unchanged since it is never used in StorageUpdateManager.checkVersion
      boolean enableStorageUpdate = false;
      this.storageVersion =
         StorageUpdateManager.checkVersion(dbSourceName, this.connFactory.getJdbcConnection(), multiDb,
            enableStorageUpdate);

      LOG.info(getInfo());
   }

   /**
    * Deletes all the files from the swap directory 
    */
   private void cleanupSwapDirectory()
   {
      PrivilegedAction<Void> action = new PrivilegedAction<Void>()
      {
         public Void run()
         {
            File[] files = swapDirectory.listFiles();
            if (files != null && files.length > 0)
            {
               LOG.info("Some files have been found in the swap directory and will be deleted");
               for (int i = 0; i < files.length; i++)
               {
                  File file = files[i];
                  // We don't use the file cleaner in case the deletion failed to ensure
                  // that the file won't be deleted while it is currently re-used
                  file.delete();
               }
            }
            return null;
         }
      };
      SecurityHelper.doPrivilegedAction(action);
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
   protected GenericConnectionFactory defaultConnectionFactory() throws NamingException, RepositoryException
   {
      // by default
      if (dbSourceName != null)
      {
         return new GenericConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider,
            maxBufferSize, swapDirectory, swapCleaner);
      }

      return new GenericConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
         valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * Prepare default DB initializer.
    * 
    * @param sqlPath
    *          - path to SQL script (database creation script)
    * @return StorageDBInitializer instance
    * @throws NamingException
    *           on JNDI error
    * @throws RepositoryException
    *           on Storage error
    * @throws IOException
    *           on I/O error
    */
   protected StorageDBInitializer defaultDBInitializer(String sqlPath) throws NamingException, RepositoryException,
      IOException
   {
      return new StorageDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
   }

   /**
    * Checks if DataSources used in right manner.
    * 
    * @param wsConfig
    *          Workspace configuration
    * @param repConfig
    *          Repository configuration
    * @throws RepositoryConfigurationException
    *           in case of configuration errors
    */
   protected void checkIntegrity(WorkspaceEntry wsConfig, RepositoryEntry repConfig)
      throws RepositoryConfigurationException
   {
      boolean isMulti;
      for (WorkspaceEntry wsEntry : repConfig.getWorkspaceEntries())
      {
         if (wsEntry.getName().equals(wsConfig.getName())
            || !wsEntry.getContainer().getType().equals(wsConfig.getContainer().getType())
            || !wsEntry.getContainer().getType().equals(this.getClass().getName()))
         {
            continue;
         }

         // MULTIDB
         if (!wsEntry.getContainer().getParameterValue(MULTIDB)
            .equals(wsConfig.getContainer().getParameterValue(MULTIDB)))
         {
            throw new RepositoryConfigurationException("All workspaces must be " + MULTIDB + " or " + SINGLEDB
               + ". But " + wsEntry.getName() + "- multi-db=" + wsEntry.getContainer().getParameterValue(MULTIDB)
               + " and " + wsConfig.getName() + "- multi-db=" + wsConfig.getContainer().getParameterValue(MULTIDB));
         }

         isMulti = Boolean.parseBoolean(wsConfig.getContainer().getParameterValue(MULTIDB));

         // source name
         String wsSourceName = null;
         String newWsSourceName = null;
         try
         {
            wsSourceName = wsEntry.getContainer().getParameterValue("sourceName");
            newWsSourceName = wsConfig.getContainer().getParameterValue("sourceName");
         }
         catch (RepositoryConfigurationException e)
         {
         }

         if (wsSourceName != null && newWsSourceName != null)
         {
            if (isMulti)
            {
               if (wsSourceName.equals(newWsSourceName))
               {
                  throw new RepositoryConfigurationException("SourceName " + wsSourceName + " alredy in use in "
                     + wsEntry.getName() + ". SourceName must be different in " + MULTIDB
                     + ". Check configuration for " + wsConfig.getName());
               }
            }
            else
            {
               if (!wsSourceName.equals(newWsSourceName))
               {
                  throw new RepositoryConfigurationException("SourceName must be equals in " + SINGLEDB + " "
                     + "repository." + " Check " + wsEntry.getName() + " and " + wsConfig.getName());
               }
            }
            continue;
         }

         // db-url
         String wsUri = null;
         String newWsUri = null;
         try
         {
            wsUri = wsEntry.getContainer().getParameterValue("db-url");
            newWsUri = wsConfig.getContainer().getParameterValue("db-url");
         }
         catch (RepositoryConfigurationException e)
         {
         }

         if (wsUri != null && newWsUri != null)
         {
            if (isMulti)
            {
               if (wsUri.equals(newWsUri))
               {
                  throw new RepositoryConfigurationException("db-url  " + wsUri + " alredy in use in "
                     + wsEntry.getName() + ". db-url must be different in " + MULTIDB + ". Check configuration for "
                     + wsConfig.getName());

               }
            }
            else
            {
               if (!wsUri.equals(newWsUri))
               {
                  throw new RepositoryConfigurationException("db-url must be equals in " + SINGLEDB + " "
                     + "repository." + " Check " + wsEntry.getName() + " and " + wsConfig.getName());
               }
            }
         }
      }
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
   protected void initDatabase() throws NamingException, RepositoryException, IOException
   {

      StorageDBInitializer dbInitializer = null;
      String sqlPath = DBInitializerHelper.scriptPath(dbDialect, multiDb);
      if (dbDialect == DBConstants.DB_DIALECT_ORACLEOCI)
      {
         LOG.warn(DBConstants.DB_DIALECT_ORACLEOCI + " dialect is experimental!");
         // sample of connection factory customization
         if (dbSourceName != null)
         {
            this.connFactory = defaultConnectionFactory();
         }
         else
         {
            this.connFactory =
               new OracleConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }
         // a particular db initializer may be configured here too
         dbInitializer = new OracleDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_ORACLE)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = new OracleDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_PGSQL)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = new PgSQLDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MYSQL || dbDialect == DBConstants.DB_DIALECT_MYSQL_UTF8
         || dbDialect == DBConstants.DB_DIALECT_MYSQL_MYISAM || dbDialect == DBConstants.DB_DIALECT_MYSQL_MYISAM_UTF8)
      {
         if (dbDialect == DBConstants.DB_DIALECT_MYSQL_MYISAM || dbDialect == DBConstants.DB_DIALECT_MYSQL_MYISAM_UTF8)
         {
            LOG.warn("MyISAM is not supported due to its lack of transaction support and integrity check, so use it only"
               + " if you don't expect any support and performances in read accesses are more important than the consistency"
               + " in your use-case. This dialect is only dedicated to the community.");
         }
         if (dbSourceName != null)
         {
            this.connFactory =
               new MySQLConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider, maxBufferSize,
                  swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new MySQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MSSQL)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DERBY)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DB2)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DB2V8)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_SYBASE)
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_INGRES)
      {
         this.connFactory = defaultConnectionFactory();
         // using Postgres initializer
         dbInitializer =
            new IngresSQLDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_HSQLDB)
      {
         if (dbSourceName != null)
         {
            this.connFactory =
               new HSQLDBConnectionFactory(getDataSource(), containerName, multiDb, valueStorageProvider,
                  maxBufferSize, swapDirectory, swapCleaner);
         }
         else
         {
            this.connFactory =
               new HSQLDBConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }
         dbInitializer = defaultDBInitializer(sqlPath);
      }
      else
      {
         // generic, DB_HSQLDB
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer(sqlPath);
      }

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

   /**
    * Return ConnectionFactory.
    * 
    * @return WorkspaceStorageConnectionFactory connection
    */
   protected GenericConnectionFactory getConnectionFactory()
   {
      return connFactory;
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection() throws RepositoryException
   {
      WorkspaceStorageConnection con = connFactory.openConnection();
      if (STATISTICS_ENABLED)
      {
         con = new StatisticsJDBCStorageConnection(con);
      }
      return con;
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      WorkspaceStorageConnection con = connFactory.openConnection(readOnly);
      if (STATISTICS_ENABLED)
      {
         con = new StatisticsJDBCStorageConnection(con);
      }
      return con;
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection reuseConnection(WorkspaceStorageConnection original) throws RepositoryException
   {
      if (original instanceof StatisticsJDBCStorageConnection)
      {
         original = ((StatisticsJDBCStorageConnection)original).getNestedWorkspaceStorageConnection();
      }

      if (original instanceof JDBCStorageConnection)
      {
         WorkspaceStorageConnectionFactory cFactory =
            new SharedConnectionFactory(((JDBCStorageConnection)original).getJdbcConnection(), containerName, multiDb,
               valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

         return STATISTICS_ENABLED ? new StatisticsJDBCStorageConnection(cFactory.openConnection(false)) : cFactory
            .openConnection(false);
      }
      else
      {
         return openConnection(false);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return containerName;
   }

   /**
    * {@inheritDoc}
    */
   public String getUniqueName()
   {
      return uniqueName;
   }

   /**
    * {@inheritDoc}
    */
   public String getInfo()
   {
      String str =
         "JDBC based JCR Workspace Data container \n" + "container name: " + containerName + " \n"
            + (isManaged ? "managed " : "") + "data source JNDI name: " + dbSourceName + "\n" + "is multi database: "
            + multiDb + "\n" + "storage version: " + storageVersion + "\n" + "value storage provider: "
            + valueStorageProvider + "\n" + "max buffer size (bytes): " + maxBufferSize + "\n"
            + "swap directory path: " + PrivilegedFileHelper.getAbsolutePath(swapDirectory);
      return str;
   }

   /**
    * {@inheritDoc}
    */
   public String getStorageVersion()
   {
      return storageVersion;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      // if isolation level lesser then TRANSACTION_READ_COMMITTED, print a warning
      Connection con = null;
      try
      {
         con = getConnectionFactory().getJdbcConnection();
         if (con.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED)
         {
            LOG.warn("Wrong default isolation level, please set the default isolation level "
               + "to READ_COMMITTED or higher. Other default isolation levels are not supported");
         }
      }
      catch (SQLException e)
      {
         LOG.error("Error checking isolation level configuration.", e);
      }
      catch (RepositoryException e)
      {
         LOG.error("Error checking isolation level configuration.", e);
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
      
      // Remove lock properties from DB. It is an issue of migration locks from 1.12.x to 1.14.x in case when we use
      // shareable cache. The lock tables will be new but still remaining lock properties in JCR tables.
      boolean deleteLocks =
         "true".equalsIgnoreCase(PrivilegedSystemHelper.getProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE,
            "false"));

      try
      {
         if (deleteLocks)
         {
            boolean failed = true;
            WorkspaceStorageConnection wsc = openConnection(false);
            if (wsc instanceof StatisticsJDBCStorageConnection)
            {
               wsc = ((StatisticsJDBCStorageConnection)wsc).getNestedWorkspaceStorageConnection();
            }
            JDBCStorageConnection conn = (JDBCStorageConnection)wsc;
            try
            {
               conn.deleteLockProperties();
               conn.commit();
               failed = false;
            }
            finally
            {
               if (failed)
               {
                  conn.rollback();
               }
            }
         }
      }
      catch (SQLException e)
      {
         LOG.error(
            "Can't remove lock properties because of " + ExceptionManagementHelper.getFullSQLExceptionMessage(e), e);
      }
      catch (RepositoryException e)
      {
         LOG.error("Can't remove lock properties because of " + e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSame(WorkspaceDataContainer another)
   {
      if (another == this)
      {
         return true;
      }

      if (another instanceof JDBCWorkspaceDataContainer)
      {
         JDBCWorkspaceDataContainer anotherJdbc = (JDBCWorkspaceDataContainer)another;

         if (getDbSourceName() != null)
         {
            // by jndi ds name
            return getDbSourceName().equals(anotherJdbc.getDbSourceName());
         }

         // by db connection params
         return getDbDriver().equals(anotherJdbc.getDbDriver()) && getDbUrl().equals(anotherJdbc.getDbUrl())
            && getDbUserName().equals(anotherJdbc.getDbUserName());
      }

      return false;
   }

   /**
    * Used in <code>equals()</code>.
    * 
    * @return DataSource name
    */
   protected String getDbSourceName()
   {
      return dbSourceName;
   }

   /**
    * Used in <code>equals()</code>.
    * 
    * @return JDBC driver
    */
   protected String getDbDriver()
   {
      return dbDriver;
   }

   /**
    * Used in <code>equals()</code>.
    * 
    * @return Database URL
    */
   protected String getDbUrl()
   {
      return dbUrl;
   }

   /**
    * Used in <code>equals()</code>.
    * 
    * @return Database username
    */
   protected String getDbUserName()
   {
      return dbUserName;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isCheckSNSNewConnection()
   {
      return checkSNSNewConnection;
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      try
      {
         DBCleanService.cleanWorkspaceData(wsConfig);

         if (wsConfig.getContainer().getValueStorages() != null)
         {
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
               public Void run() throws IOException, RepositoryConfigurationException
               {
                  for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
                  {
                     File valueStorageDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
                     if (valueStorageDir.exists())
                     {
                        DirectoryHelper.removeDirectory(valueStorageDir);
                     }
                  }

                  return null;
               }
            });
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupException(e);
      }
      catch (NamingException e)
      {
         throw new BackupException(e);
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      catch (PrivilegedActionException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void backup(final File storageDir) throws BackupException
   {
      ObjectWriter backupInfo = null;

      try
      {
         backupInfo =
            new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(new File(storageDir,
               "JDBCWorkspaceDataContainer.info")));

         backupInfo.writeString(containerName);
         backupInfo.writeBoolean(multiDb);

         Map<String, String> scripts = new HashMap<String, String>();

         if (multiDb)
         {
            scripts.put("JCR_MITEM", "select * from JCR_MITEM where JCR_MITEM.NAME <> '" + Constants.ROOT_PARENT_NAME
               + "'");
            scripts.put("JCR_MVALUE", "select * from JCR_MVALUE");
            scripts.put("JCR_MREF", "select * from JCR_MREF");
         }
         else
         {
            scripts.put("JCR_SITEM", "select * from JCR_SITEM where CONTAINER_NAME='" + containerName + "'");
            scripts.put("JCR_SVALUE",
               "select V.* from JCR_SVALUE V, JCR_SITEM I where I.ID=V.PROPERTY_ID and I.CONTAINER_NAME='"
                  + containerName + "'");
            scripts.put("JCR_SREF",
               "select R.* from JCR_SREF R, JCR_SITEM I where I.ID=R.PROPERTY_ID and I.CONTAINER_NAME='"
                  + containerName + "'");
         }

         // using existing DataSource to get a JDBC Connection.
         Connection jdbcConn = connFactory.getJdbcConnection();

         DBBackup.backup(storageDir, jdbcConn, scripts);

         // backup value storage
         if (wsConfig.getContainer().getValueStorages() != null)
         {
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
               public Void run() throws IOException, RepositoryConfigurationException
               {
                  for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
                  {
                     File srcDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
                     if (!srcDir.exists())
                     {
                        throw new IOException("Can't backup value storage. Directory " + srcDir.getName()
                           + " doesn't exists");
                     }
                     else
                     {
                        File zipFile = new File(storageDir, "values-" + valueStorage.getId() + ".zip");
                        DirectoryHelper.compressDirectory(srcDir, zipFile);
                     }
                  }

                  return null;
               }
            });
         }
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (RepositoryException e)
      {
         throw new BackupException(e);
      }
      catch (PrivilegedActionException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (backupInfo != null)
         {
            try
            {
               backupInfo.close();
            }
            catch (IOException e)
            {
               throw new BackupException(e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public DataRestore getDataRestorer(DataRestoreContext context) throws BackupException
   {

      List<DataRestore> restorers = new ArrayList<DataRestore>();

      ObjectReader backupInfo = null;
      try
      {
         File storageDir = (File)context.getObject(DataRestoreContext.STORAGE_DIR);
         Connection jdbcConn = null;

         if (context.getObject(DataRestoreContext.DB_CONNECTION) == null)
         {
            try
            {
               jdbcConn = connFactory.getJdbcConnection();
               jdbcConn.setAutoCommit(false);
            }
            catch (SQLException e)
            {
               throw new BackupException(e);
            }
            catch (RepositoryException e)
            {
               throw new BackupException(e);
            }

         }
         else
         {
            jdbcConn = (Connection)context.getObject(DataRestoreContext.DB_CONNECTION);
         }

         backupInfo =
            new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(new File(storageDir,
               "JDBCWorkspaceDataContainer.info")));

         String srcContainerName = backupInfo.readString();
         boolean srcMultiDb = backupInfo.readBoolean();

         Map<String, RestoreTableRule> tables = new LinkedHashMap<String, RestoreTableRule>();

         // ITEM table
         String dstTableName = "JCR_" + (multiDb ? "M" : "S") + "ITEM";
         String srcTableName = "JCR_" + (srcMultiDb ? "M" : "S") + "ITEM";

         RestoreTableRule restoreTableRule = new RestoreTableRule();
         restoreTableRule.setSrcContainerName(srcContainerName);
         restoreTableRule.setSrcMultiDb(srcMultiDb);
         restoreTableRule.setDstContainerName(containerName);
         restoreTableRule.setDstMultiDb(multiDb);
         restoreTableRule.setSrcTableName(srcTableName);

         if (multiDb)
         {
            if (!srcMultiDb)
            {
               // CONTAINER_NAME column index
               restoreTableRule.setDeleteColumnIndex(4);

               // ID and PARENT_ID column indexes
               Set<Integer> convertColumnIndex = new HashSet<Integer>();
               convertColumnIndex.add(0);
               convertColumnIndex.add(1);
               restoreTableRule.setConvertColumnIndex(convertColumnIndex);
            }
         }
         else
         {
            if (srcMultiDb)
            {
               // CONTAINER_NAME column index
               restoreTableRule.setNewColumnIndex(4);
               restoreTableRule.setNewColumnName("CONTAINER_NAME");
               restoreTableRule.setNewColumnType(Types.VARCHAR);

               // ID and PARENT_ID column indexes
               Set<Integer> convertColumnIndex = new HashSet<Integer>();
               convertColumnIndex.add(0);
               convertColumnIndex.add(1);
               restoreTableRule.setConvertColumnIndex(convertColumnIndex);
            }
            else
            {
               // ID and PARENT_ID and CONTAINER_NAME column indexes
               Set<Integer> convertColumnIndex = new HashSet<Integer>();
               convertColumnIndex.add(0);
               convertColumnIndex.add(1);
               convertColumnIndex.add(4);
               restoreTableRule.setConvertColumnIndex(convertColumnIndex);
            }
         }
         tables.put(dstTableName, restoreTableRule);

         // VALUE table
         dstTableName = "JCR_" + (multiDb ? "M" : "S") + "VALUE";
         srcTableName = "JCR_" + (srcMultiDb ? "M" : "S") + "VALUE";

         restoreTableRule = new RestoreTableRule();
         restoreTableRule.setSrcContainerName(srcContainerName);
         restoreTableRule.setSrcMultiDb(srcMultiDb);
         restoreTableRule.setDstContainerName(containerName);
         restoreTableRule.setDstMultiDb(multiDb);
         restoreTableRule.setSrcTableName(srcTableName);

         // auto increment ID column
         restoreTableRule.setSkipColumnIndex(0);

         if (!multiDb || !srcMultiDb)
         {
            // PROPERTY_ID column index
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(3);
            restoreTableRule.setConvertColumnIndex(convertColumnIndex);
         }
         tables.put(dstTableName, restoreTableRule);

         // REF tables
         dstTableName = "JCR_" + (multiDb ? "M" : "S") + "REF";
         srcTableName = "JCR_" + (srcMultiDb ? "M" : "S") + "REF";

         restoreTableRule = new RestoreTableRule();
         restoreTableRule.setSrcContainerName(srcContainerName);
         restoreTableRule.setSrcMultiDb(srcMultiDb);
         restoreTableRule.setDstContainerName(containerName);
         restoreTableRule.setDstMultiDb(multiDb);
         restoreTableRule.setSrcTableName(srcTableName);

         if (!multiDb || !srcMultiDb)
         {
            // NODE_ID and PROPERTY_ID column indexes
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(0);
            convertColumnIndex.add(1);
            restoreTableRule.setConvertColumnIndex(convertColumnIndex);
         }
         tables.put(dstTableName, restoreTableRule);

         DBCleaner dbCleaner = null;
         if (context.getObject(DataRestoreContext.DB_CLEANER) != null)
         {
            dbCleaner = (DBCleaner)context.getObject(DataRestoreContext.DB_CLEANER);
         }
         else
         {
            dbCleaner = DBCleanService.getWorkspaceDBCleaner(jdbcConn, wsConfig);
         }

         restorers.add(new DBRestore(storageDir, jdbcConn, tables, wsConfig, swapCleaner, dbCleaner));

         // prepare value storage restorer
         if (wsConfig.getContainer().getValueStorages() != null)
         {
            List<File> dataDirs = new ArrayList<File>();
            List<File> backupDirs = new ArrayList<File>();

            List<ValueStorageEntry> valueStorages = wsConfig.getContainer().getValueStorages();
            for (ValueStorageEntry valueStorage : valueStorages)
            {
               File dataDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
               dataDirs.add(dataDir);

               File zipFile = new File(storageDir, "values-" + valueStorage.getId() + ".zip");
               if (PrivilegedFileHelper.exists(zipFile))
               {
                  backupDirs.add(zipFile);
               }
               else
               {
                  // try to check if we have deal with old backup format
                  zipFile = new File(storageDir, "values/" + valueStorage.getId());
                  if (PrivilegedFileHelper.exists(zipFile))
                  {
                     backupDirs.add(zipFile);
                  }
                  else
                  {
                     throw new RepositoryConfigurationException("There is no backup data for value storage with id "
                        + valueStorage.getId());
                  }
               }
            }

            restorers.add(new DirectoryRestore(dataDirs, backupDirs));
         }

         return new ComplexDataRestore(restorers);
      }
      catch (FileNotFoundException e)
      {
         throw new BackupException(e);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (NamingException e)
      {
         throw new BackupException(e);
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (backupInfo != null)
         {
            try
            {
               backupInfo.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close object reader", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeDataIndexingIterator getNodeDataIndexingIterator(int pageSize) throws RepositoryException
   {
      if (isReindexingSupport())
      {
         return new JdbcNodeDataIndexingIterator(connFactory, pageSize);
      }
      throw new UnsupportedOperationException(
         "The method getNodeDataIndexingIterator is not supported for this type of connection "
            + "use the complex queries instead");
   }

   /**
    * {@inheritDoc}
    */
   public boolean isReindexingSupport()
   {
      return connFactory.isReindexingSupported();
   }
   
   /**
    * Get the data source from the InitialContext and wraps it into a {@link ManagedDataSource}
    * in case it has been configured as managed
    */
   protected DataSource getDataSource() throws RepositoryException
   {
      try
      {
         return dsProvider.getDataSource(dbSourceName);
      }
      catch (NamingException e)
      {
         throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.", e);
      }
   }
}
