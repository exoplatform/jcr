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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.ComplexDataRestor;
import org.exoplatform.services.jcr.impl.backup.DataRestor;
import org.exoplatform.services.jcr.impl.backup.JdbcBackupable;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBBackup;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBRestor;
import org.exoplatform.services.jcr.impl.backup.rdbms.DirectoryRestor;
import org.exoplatform.services.jcr.impl.backup.rdbms.RestoreTableRule;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
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
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picocontainer.Startable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id:GenericWorkspaceDataContainer.java 13433 2007-03-15 16:07:23Z peterit $
 */
public class JDBCWorkspaceDataContainer extends WorkspaceDataContainerBase implements Startable, JdbcBackupable,
   Reindexable
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainer");

   /**
    * Indicates if the statistics has to be enabled.
    */
   public static final boolean STATISTICS_ENABLED =
      Boolean.valueOf(PrivilegedSystemHelper.getProperty("JDBCWorkspaceDataContainer.statistics.enabled"));
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
    *          External Value Stprages provider
    * @throws RepositoryConfigurationException
    *           if Repository configuration is wrong
    * @throws NamingException
    *           if JNDI exception (on DataSource lookup)
    */
   public JDBCWorkspaceDataContainer(WorkspaceEntry wsConfig, RepositoryEntry repConfig,
      InitialContextInitializer contextInit, ValueStoragePluginProvider valueStorageProvider,
      FileCleanerHolder fileCleanerHolder) throws RepositoryConfigurationException, NamingException,
      RepositoryException, IOException
   {

      // This recall is workaround for tenants creation. There is a trouble in visibility datasource
      // binded in one tomcat context from another tomcat context. 
      contextInit.recall();

      checkIntegrity(wsConfig, repConfig);
      this.wsConfig = wsConfig;
      this.containerName = wsConfig.getName();
      this.uniqueName = wsConfig.getUniqueName();
      this.multiDb = Boolean.parseBoolean(wsConfig.getContainer().getParameterValue(MULTIDB));
      this.valueStorageProvider = valueStorageProvider;

      // ------------- Database config ------------------
      String pDbDialect = null;
      try
      {
         pDbDialect = validateDialect(wsConfig.getContainer().getParameterValue(DB_DIALECT));
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
         LOG.info("Connect to JCR database as user '" + this.dbUserName + "'");

         if (pDbDialect == DBConstants.DB_DIALECT_GENERIC || DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(pDbDialect))
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
            sn = wsConfig.getContainer().getParameterValue("sourceName"); // TODO for backward comp,
            // remove in rel.2.0
         }
         this.dbSourceName = sn;

         if (pDbDialect == DBConstants.DB_DIALECT_GENERIC)
         {
            // try to detect via JDBC metadata
            final DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
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
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
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
         this.checkSNSNewConnection = DBConstants.DB_DIALECT_SYBASE.equals(this.dbDialect) ? false : true;
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

      this.swapCleaner = fileCleanerHolder.getFileCleaner();

      initDatabase();

      String suParam = null;
      boolean enableStorageUpdate = false;
      try
      {
         suParam = wsConfig.getContainer().getParameterValue("update-storage");
         enableStorageUpdate = Boolean.parseBoolean(suParam);
      }
      catch (RepositoryConfigurationException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("update-storage parameter is not set " + dbSourceName);
         }
      }

      this.storageVersion =
         StorageUpdateManager.checkVersion(dbSourceName, this.connFactory.getJdbcConnection(), multiDb,
            enableStorageUpdate);

      LOG.info(getInfo());
   }

   /**
    * Prepare sefault connection factory.
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
         DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
         if (ds != null)
         {
            return new GenericConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
               swapDirectory, swapCleaner);
         }

         throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
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
         if (!wsEntry.getContainer().getParameterValue(MULTIDB).equals(
            wsConfig.getContainer().getParameterValue(MULTIDB)))
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

      StorageDBInitializer dbInitilizer = null;
      String sqlPath = null;
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

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ora.sql";

         // a particular db initializer may be configured here too
         dbInitilizer = new OracleDBInitializer(containerName, this.connFactory.getJdbcConnection(), sqlPath, multiDb);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_ORACLE)
      {
         this.connFactory = defaultConnectionFactory();
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
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               this.connFactory =
                  new MySQLConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            }
            else
            {
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
            }
         }
         else
         {
            this.connFactory =
               new MySQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MYSQL_UTF8)
      {
         // [PN] 13.07.08
         if (dbSourceName != null)
         {
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               this.connFactory =
                  new MySQLConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            }
            else
            {
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
            }
         }
         else
         {
            this.connFactory =
               new MySQLConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }

         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-utf8.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_MSSQL)
      {
         this.connFactory = defaultConnectionFactory();
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
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_DB2V8)
      {
         this.connFactory = defaultConnectionFactory();
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2v8.sql";
         dbInitilizer = defaultDBInitializer(sqlPath);
      }
      else if (dbDialect == DBConstants.DB_DIALECT_SYBASE)
      {
         this.connFactory = defaultConnectionFactory();
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
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               this.connFactory =
                  new HSQLDBConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            }
            else
            {
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
            }
         }
         else
         {
            this.connFactory =
               new HSQLDBConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
         }
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
    * Return ConnectionFactory.
    * 
    * @return WorkspaceStorageConnectionFactory connection
    */
   protected GenericConnectionFactory getConnectionFactory()
   {
      return connFactory;
   }

   protected String validateDialect(String confParam)
   {
      for (String dbType : DBConstants.DB_DIALECTS)
      {
         if (dbType.equalsIgnoreCase(confParam))
         {
            return dbType;
         }
      }

      return DBConstants.DB_DIALECT_GENERIC; // by default
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

         return STATISTICS_ENABLED ? new StatisticsJDBCStorageConnection(cFactory.openConnection()) : cFactory
            .openConnection();
      }
      else
      {
         return openConnection();
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
            + "data source JNDI name: " + dbSourceName + "\n" + "is multi database: " + multiDb + "\n"
            + "storage version: " + storageVersion + "\n" + "value storage provider: " + valueStorageProvider + "\n"
            + "max buffer size (bytes): " + maxBufferSize + "\n" + "swap directory path: "
            + PrivilegedFileHelper.getAbsolutePath(swapDirectory);
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
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {

      // TODO HSQLDB Stop (debug)
      // if (dbDialect.equals(DB_DIALECT_GENERIC) ||
      // dbDialect.equals(DB_DIALECT_HSQLDB)) {
      // // shutdown in-process HSQLDB database
      // System.out.println("Shutdown in-process HSQLDB database...");
      // try {
      // JDBCStorageConnection conn = (JDBCStorageConnection) openConnection();
      // Connection jdbcConn = conn.getJdbcConnection();
      // String dbUrl = jdbcConn.getMetaData().getURL();
      // if (dbUrl.startsWith("jdbc:hsqldb:file") ||
      // dbUrl.startsWith("jdbc:hsqldb:mem")) {
      // // yeah, there is in-process hsqldb, shutdown it now
      // jdbcConn.createStatement().execute("SHUTDOWN");
      // System.out.println("Shutdown in-process HSQLDB database... done.");
      // }
      // } catch (Throwable e) {
      // log.error("JDBC Data container stop error " + e);
      // e.printStackTrace();
      // }
      // }
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
            for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
            {
               File valueStorageDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
               if (PrivilegedFileHelper.exists(valueStorageDir))
               {
                  DirectoryHelper.removeDirectory(valueStorageDir);
               }
            }
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
      catch (IOException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void backup(File storageDir) throws BackupException
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
            scripts.put("JCR_MITEM", "select * from JCR_MITEM where JCR_MITEM.name <> '" + Constants.ROOT_PARENT_NAME
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

         final DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
         if (ds == null)
         {
            throw new NameNotFoundException("Data source " + dbSourceName + " not found");
         }

         Connection jdbcConn =
            SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
            {
               public Connection run() throws Exception
               {
                  return ds.getConnection();

               }
            });

         DBBackup.backup(storageDir, jdbcConn, scripts);

         // backup value storage
         if (wsConfig.getContainer().getValueStorages() != null)
         {
            for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
            {
               File srcDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
               if (!PrivilegedFileHelper.exists(srcDir))
               {
                  throw new BackupException("Can't backup value storage. Directory " + srcDir.getName()
                     + " doesn't exists");
               }
               else
               {
                  File destValuesDir = new File(storageDir, "values");
                  File destDir = new File(destValuesDir, valueStorage.getId());

                  DirectoryHelper.compressDirectory(srcDir, destDir);
               }
            }
         }
      }
      catch (IOException e)
      {
         throw new BackupException(e);
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
   public DataRestor getDataRestorer(File storageDir, Connection jdbcConn) throws BackupException
   {
      List<DataRestor> restorers = new ArrayList<DataRestor>();

      ObjectReader backupInfo = null;
      try
      {
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
         restoreTableRule.setContentFile(new File(storageDir, srcTableName + DBBackup.CONTENT_FILE_SUFFIX));
         restoreTableRule.setContentLenFile(new File(storageDir, srcTableName + DBBackup.CONTENT_LEN_FILE_SUFFIX));

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
         restoreTableRule.setContentFile(new File(storageDir, srcTableName + DBBackup.CONTENT_FILE_SUFFIX));
         restoreTableRule.setContentLenFile(new File(storageDir, srcTableName + DBBackup.CONTENT_LEN_FILE_SUFFIX));

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
         restoreTableRule.setContentFile(new File(storageDir, srcTableName + DBBackup.CONTENT_FILE_SUFFIX));
         restoreTableRule.setContentLenFile(new File(storageDir, srcTableName + DBBackup.CONTENT_LEN_FILE_SUFFIX));

         if (!multiDb || !srcMultiDb)
         {
            // NODE_ID and PROPERTY_ID column indexes
            Set<Integer> convertColumnIndex = new HashSet<Integer>();
            convertColumnIndex.add(0);
            convertColumnIndex.add(1);
            restoreTableRule.setConvertColumnIndex(convertColumnIndex);
         }
         tables.put(dstTableName, restoreTableRule);
         
         restorers.add(new DBRestor(storageDir, jdbcConn, tables, wsConfig, swapCleaner));

         // prepare value storage restorer
         File backupValueStorageDir = new File(storageDir, "values");
         if (wsConfig.getContainer().getValueStorages() != null)
         {
            List<File> dataDirs = new ArrayList<File>();
            List<File> backupDirs = new ArrayList<File>();

            List<ValueStorageEntry> valueStorages = wsConfig.getContainer().getValueStorages();
            String[] valueStoragesFiles = PrivilegedFileHelper.list(backupValueStorageDir);

            if ((valueStoragesFiles == null && valueStorages.size() != 0)
               || (valueStoragesFiles != null && valueStoragesFiles.length != valueStorages.size()))
            {
               throw new RepositoryConfigurationException("Workspace configuration [" + wsConfig.getName()
                  + "] has a different amount of value storages than exist in backup");
            }

            for (ValueStorageEntry valueStorage : valueStorages)
            {
               File backupDir = new File(backupValueStorageDir, valueStorage.getId());
               if (!PrivilegedFileHelper.exists(backupDir))
               {
                  throw new RepositoryConfigurationException("Can't restore value storage. Directory "
                     + backupDir.getName() + " doesn't exists");
               }
               else
               {
                  File dataDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));

                  dataDirs.add(dataDir);
                  backupDirs.add(backupDir);
               }
            }

            restorers.add(new DirectoryRestor(dataDirs, backupDirs));
         }
         else
         {
            if (PrivilegedFileHelper.exists(backupValueStorageDir))
            {
               throw new RepositoryConfigurationException("Value storage didn't configure in workspace ["
                  + wsConfig.getName() + "] configuration but value storage backup files exist");
            }
         }

         return new ComplexDataRestor(restorers);
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
   public DataRestor getDataRestorer(File storageDir) throws BackupException
   {
      try
      {
         final DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);

         if (ds != null)
         {
            Connection jdbcConn =
               SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
            {
               public Connection run() throws Exception
               {
                  return ds.getConnection();
               }
            });
            jdbcConn.setAutoCommit(false);

            return getDataRestorer(storageDir, jdbcConn);
         }
         else
         {
            throw new NameNotFoundException("Data source " + dbSourceName + " not found");
         }
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      catch (NamingException e)
      {
         throw new BackupException(e);
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
      throw new UnsupportedOperationException("The method getNodeDataIndexingIterator is not supported for this type of connection use the complex queries instead");
   }

   /**
    * {@inheritDoc}
    */
   public boolean isReindexingSupport()
   {
      return connFactory.isReindexingSupport();
   }
}
