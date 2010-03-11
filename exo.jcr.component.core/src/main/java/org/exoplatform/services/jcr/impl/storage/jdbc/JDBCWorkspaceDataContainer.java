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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.HSQLDBConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.MySQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.OracleConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.IngresSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.OracleDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.PgSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.StorageDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.statistics.StatisticsJDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.update.StorageUpdateManager;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id:GenericWorkspaceDataContainer.java 13433 2007-03-15 16:07:23Z peterit $
 */
public class JDBCWorkspaceDataContainer extends WorkspaceDataContainerBase implements Startable
{

   protected static final Log LOG = ExoLogger.getLogger("jcr.JDBCWorkspaceDataContainer");

   /**
    * Indicates if the statistics has to be enabled.
    */
   public static final boolean STATISTICS_ENABLED = Boolean.valueOf(System.getProperty("JDBCWorkspaceDataContainer.statistics.enabled"));
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

   protected final String containerName;

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
      InitialContextInitializer contextInit, ValueStoragePluginProvider valueStorageProvider)
      throws RepositoryConfigurationException, NamingException, RepositoryException, IOException
   {

      checkIntegrity(wsConfig, repConfig);

      this.containerName = wsConfig.getName();
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
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               Connection jdbcConn = null;
               try
               {
                  jdbcConn = ds.getConnection();
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

      try
      {
         this.checkSNSNewConnection = wsConfig.getContainer().getParameterBoolean(CHECK_SNS_NEW_CONNECTION);
      }
      catch (RepositoryConfigurationException e)
      {
         this.checkSNSNewConnection = true;
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
      if (!swapDirectory.exists())
         swapDirectory.mkdirs();

      this.swapCleaner = new FileCleaner(false);

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
            LOG.debug("update-storage parameter is not set " + dbSourceName);
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
            return new GenericConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
               swapDirectory, swapCleaner);

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
            continue;

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
            this.connFactory = defaultConnectionFactory();
         else
            this.connFactory =
               new OracleConnectionFactory(dbDriver, dbUrl, dbUserName, dbPassword, containerName, multiDb,
                  valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);

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
               this.connFactory =
                  new MySQLConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            else
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
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
            DataSource ds = (DataSource)new InitialContext().lookup(dbSourceName);
            if (ds != null)
               this.connFactory =
                  new MySQLConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            else
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
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
               this.connFactory =
                  new HSQLDBConnectionFactory(ds, containerName, multiDb, valueStorageProvider, maxBufferSize,
                     swapDirectory, swapCleaner);
            else
               throw new RepositoryException("Datasource '" + dbSourceName + "' is not bound in this context.");
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

         return STATISTICS_ENABLED ? new StatisticsJDBCStorageConnection(cFactory.openConnection()) : cFactory.openConnection();
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
   public String getInfo()
   {
      String str =
         "JDBC based JCR Workspace Data container \n" + "container name: " + containerName + " \n"
            + "data source JNDI name: " + dbSourceName + "\n" + "is multi database: " + multiDb + "\n"
            + "storage version: " + storageVersion + "\n" + "value storage provider: " + valueStorageProvider + "\n"
            + "max buffer size (bytes): " + maxBufferSize + "\n" + "swap directory path: "
            + swapDirectory.getAbsolutePath();
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
      this.swapCleaner.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      this.swapCleaner.halt();
      this.swapCleaner.interrupt();

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
         return true;

      if (another instanceof JDBCWorkspaceDataContainer)
      {
         JDBCWorkspaceDataContainer anotherJdbc = (JDBCWorkspaceDataContainer)another;

         if (getDbSourceName() != null)
            // by jndi ds name
            return getDbSourceName().equals(anotherJdbc.getDbSourceName());

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

}
