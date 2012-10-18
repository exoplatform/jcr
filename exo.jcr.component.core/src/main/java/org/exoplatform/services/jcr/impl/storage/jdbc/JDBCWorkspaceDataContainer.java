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
import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
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
import org.exoplatform.services.jcr.impl.backup.rdbms.TableTransformationRule;
import org.exoplatform.services.jcr.impl.backup.rdbms.TableTransformationRuleGenerator;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.query.NodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.core.query.Reindexable;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.HSQLDBConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.MySQLConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.indexing.JdbcNodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.IngresSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.OracleDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.PgSQLDBInitializer;
import org.exoplatform.services.jcr.impl.storage.jdbc.statistics.StatisticsJDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
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
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

   public final static String SOURCE_NAME = "source-name";

   /**
    * Data structure type
    */
   public final static String DB_STRUCTURE_TYPE = "db-structure-type";

   /**
    * Suffix used in tables names when isolated-databse structure used 
    */
   public final static String DB_TABLENAME_SUFFIX = "db-tablename-suffix";

   /**
    * Describe which type of RDBMS will be used (DB creation metadata etc.)
    */
   public final static String DB_DIALECT = "dialect";

   public final static String DB_FORCE_QUERY_HINTS = "force.query.hints";

   protected JDBCDataContainerConfig containerConfig;

   public GenericConnectionFactory connFactory;

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
       */
      SharedConnectionFactory(Connection connection, JDBCDataContainerConfig containerConfig)
      {
         super(null, containerConfig);
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
      DataSourceProvider dsProvider, FileCleanerHolder fileCleanerHolder) throws RepositoryConfigurationException,
      NamingException, RepositoryException, IOException
   {
      checkIntegrity(wsConfig, repConfig);
      this.wsConfig = wsConfig;
      this.containerConfig = new JDBCDataContainerConfig();
      this.containerConfig.containerName = wsConfig.getName();
      this.containerConfig.uniqueName = wsConfig.getUniqueName();

      this.containerConfig.dbStructureType = DBInitializerHelper.getDatabaseType(wsConfig);
      this.containerConfig.dbTableSuffix = DBInitializerHelper.getDBTableSuffix(wsConfig);

      this.containerConfig.valueStorageProvider = valueStorageProvider;
      this.containerConfig.dsProvider = dsProvider;

      // ------------- Database config ------------------
      String pDbDialect = validateDialect(DBInitializerHelper.getDatabaseDialect(wsConfig));

      this.containerConfig.dbSourceName = wsConfig.getContainer().getParameterValue(SOURCE_NAME);

      if (dsProvider == null)
      {
         throw new IllegalArgumentException(
            "Since a data source has been defined, the DataSourceProvider cannot be null, add it in your configuration.");
      }
      // the data source cannot be managed if there is no transaction manager
      this.containerConfig.isManaged = dsProvider.isManaged(containerConfig.dbSourceName);

      if (pDbDialect.startsWith(DBConstants.DB_DIALECT_AUTO))
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

            this.containerConfig.dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
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
         this.containerConfig.dbDialect = pDbDialect;
      }
      // check is there DB_FORCE_QUERY_HINTS parameter - by default its enabled
      containerConfig.useQueryHints = wsConfig.getContainer().getParameterBoolean(DB_FORCE_QUERY_HINTS, true);

      try
      {
         this.containerConfig.checkSNSNewConnection =
            wsConfig.getContainer().getParameterBoolean(CHECK_SNS_NEW_CONNECTION);
      }
      catch (RepositoryConfigurationException e)
      {
         // don't use new connection by default
         this.containerConfig.checkSNSNewConnection = false;
      }

      // ------------- Spool config ------------------
      this.containerConfig.spoolConfig = new SpoolConfig(fileCleanerHolder.getFileCleaner());
      try
      {
         this.containerConfig.spoolConfig.maxBufferSize =
            wsConfig.getContainer().getParameterInteger(MAXBUFFERSIZE_PROP);
      }
      catch (RepositoryConfigurationException e)
      {
         this.containerConfig.spoolConfig.maxBufferSize = DEF_MAXBUFFERSIZE;
      }

      try
      {
         String sdParam = wsConfig.getContainer().getParameterValue(SWAPDIR_PROP);
         this.containerConfig.spoolConfig.tempDirectory = new File(sdParam);
      }
      catch (RepositoryConfigurationException e1)
      {
         this.containerConfig.spoolConfig.tempDirectory = new File(DEF_SWAPDIR);
      }
      if (!PrivilegedFileHelper.exists(this.containerConfig.spoolConfig.tempDirectory))
      {
         PrivilegedFileHelper.mkdirs(this.containerConfig.spoolConfig.tempDirectory);
      }

      this.containerConfig.initScriptPath =
         DBInitializerHelper.scriptPath(containerConfig.dbDialect, containerConfig.dbStructureType.isMultiDatabase());

      LOG.info(getInfo());

      initDatabase();
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
      return new GenericConnectionFactory(getDataSource(), containerConfig);
   }

   /**
    * Prepare default DB initializer.
    * 
    * @param sqlPath
    *          - path to SQL script (database creation script)
    * @return DBInitializer instance
    * @throws NamingException
    *           on JNDI error
    * @throws RepositoryException
    *           on Storage error
    * @throws IOException
    *           on I/O error
    */
   protected DBInitializer defaultDBInitializer() throws NamingException, RepositoryException, IOException
   {
      return new DBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
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
      DatabaseStructureType dbType = DBInitializerHelper.getDatabaseType(wsConfig);

      for (WorkspaceEntry wsEntry : repConfig.getWorkspaceEntries())
      {
         if (wsEntry.getName().equals(wsConfig.getName())
            || !wsEntry.getContainer().getType().equals(wsConfig.getContainer().getType())
            || !wsEntry.getContainer().getType().equals(this.getClass().getName()))
         {
            continue;
         }

         if (!DBInitializerHelper.getDatabaseType(wsEntry).equals(dbType))
         {
            throw new RepositoryConfigurationException("All workspaces must be of same DB type. But "
               + wsEntry.getName() + "=" + DBInitializerHelper.getDatabaseType(wsEntry) + " and " + wsConfig.getName()
               + "=" + dbType);
         }

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
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }

         if (wsSourceName != null && newWsSourceName != null)
         {
            if (dbType.isShareSameDatasource())
            {
               if (!wsSourceName.equals(newWsSourceName))
               {
                  throw new RepositoryConfigurationException("SourceName must be equals in " + dbType
                     + "-database repository." + " Check " + wsEntry.getName() + " and " + wsConfig.getName());
               }
            }
            else
            {
               if (wsSourceName.equals(newWsSourceName))
               {
                  throw new RepositoryConfigurationException("SourceName " + wsSourceName + " already in use in "
                     + wsEntry.getName() + ". SourceName must be different in " + dbType
                     + "-database structure type. Check configuration for " + wsConfig.getName());
               }
            }

            continue;
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
      DBInitializer dbInitializer = null;
      if (containerConfig.dbDialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         LOG.warn(DBConstants.DB_DIALECT_ORACLEOCI + " dialect is experimental!");

         this.connFactory = defaultConnectionFactory();
         dbInitializer = new OracleDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_ORACLE))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = new OracleDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_PGSQL))
      {
         this.connFactory = defaultConnectionFactory();
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
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_MSSQL))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_DERBY))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_DB2))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
      {
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_INGRES))
      {
         this.connFactory = defaultConnectionFactory();
         // using Postgres initializer
         dbInitializer = new IngresSQLDBInitializer(this.connFactory.getJdbcConnection(), containerConfig);
      }
      else if (containerConfig.dbDialect.startsWith(DBConstants.DB_DIALECT_HSQLDB))
      {
         this.connFactory = new HSQLDBConnectionFactory(getDataSource(), containerConfig);
         dbInitializer = defaultDBInitializer();
      }
      else
      {
         // generic, DB_HSQLDB
         this.connFactory = defaultConnectionFactory();
         dbInitializer = defaultDBInitializer();
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
            new SharedConnectionFactory(((JDBCStorageConnection)original).getJdbcConnection(), containerConfig);

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
      return containerConfig.containerName;
   }

   /**
    * {@inheritDoc}
    */
   public String getUniqueName()
   {
      return containerConfig.uniqueName;
   }

   /**
    * {@inheritDoc}
    */
   public String getInfo()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("dialect:");
      builder.append(this.containerConfig.dbDialect);
      builder.append(", ");

      for (SimpleParameterEntry element : wsConfig.getContainer().getParameters())
      {
         if (!element.getName().equals("dialect"))
         {
            builder.append(element.getName());
            builder.append(":");
            builder.append(element.getValue());
            builder.append(", ");
         }
      }
      builder.append("value storage provider: ");
      builder.append(containerConfig.valueStorageProvider);

      return builder.toString();
   }

   /**
    * {@inheritDoc}
    */
   public String getStorageVersion()
   {
      return containerConfig.storageVersion;
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
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
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
         LOG.error("Can't remove lock properties because of " + JDBCUtils.getFullMessage(e), e);
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

         return getDbSourceName().equals(anotherJdbc.getDbSourceName());
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
      return containerConfig.dbSourceName;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isCheckSNSNewConnection()
   {
      return containerConfig.checkSNSNewConnection;
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
      catch (DBCleanException e)
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

         backupInfo.writeString(containerConfig.containerName);
         backupInfo.writeString(containerConfig.dbStructureType.toString());

         Map<String, String> scripts = new HashMap<String, String>();

         String itemTable = DBInitializerHelper.getItemTableName(containerConfig);
         String valueTable = DBInitializerHelper.getValueTableName(containerConfig);
         String refTable = DBInitializerHelper.getRefTableName(containerConfig);

         backupInfo.writeString(itemTable);
         backupInfo.writeString(valueTable);
         backupInfo.writeString(refTable);

         if (containerConfig.dbStructureType.isMultiDatabase())
         {
            scripts
               .put(itemTable, "select * from " + itemTable + " where NAME <> '" + Constants.ROOT_PARENT_NAME + "'");
            scripts.put(valueTable, "select * from " + valueTable);
            scripts.put(refTable, "select * from " + refTable);
         }
         else
         {
            scripts.put(itemTable, "select * from " + itemTable + " where CONTAINER_NAME='"
               + containerConfig.containerName + "'");
            scripts.put(valueTable, "select V.* from " + valueTable + " V, " + itemTable
               + " I where I.ID=V.PROPERTY_ID and I.CONTAINER_NAME='" + containerConfig.containerName + "'");
            scripts.put(refTable, "select R.* from " + refTable + " R, " + itemTable
               + " I where I.ID=R.PROPERTY_ID and I.CONTAINER_NAME='" + containerConfig.containerName + "'");
         }

         // using existing DataSource to get a JDBC Connection.
         Connection jdbcConn = connFactory.getJdbcConnection();

         DBBackup.backup(storageDir, jdbcConn, scripts);

         // backup value storage
         if (wsConfig.getContainer().getValueStorages() != null)
         {
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
               public Void run() throws RepositoryConfigurationException, IOException
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
   public DataRestore getDataRestorer(DataRestoreContext dataRestoreContext) throws BackupException
   {
      try
      {
         List<DataRestore> restorers = new ArrayList<DataRestore>();
         Map<String, TableTransformationRule> tables = new LinkedHashMap<String, TableTransformationRule>();

         Connection jdbcConn = getJdbcConnection(dataRestoreContext);
         DBCleanerTool dbCleaner = getDbCleaner(dataRestoreContext, jdbcConn);
         File storageDir = getStorageDir(dataRestoreContext);

         TableTransformationRuleGenerator tableTransformationRuleGenerator =
            new TableTransformationRuleGenerator(containerConfig, storageDir);

         tables.put(DBInitializerHelper.getItemTableName(containerConfig),
            tableTransformationRuleGenerator.getItemTableTransformationRule());
         tables.put(DBInitializerHelper.getValueTableName(containerConfig),
            tableTransformationRuleGenerator.getValueTableTransformationRule());
         tables.put(DBInitializerHelper.getRefTableName(containerConfig),
            tableTransformationRuleGenerator.getRefTableTransformationRule());

         restorers.add(new DBRestore(storageDir, jdbcConn, tables, wsConfig, containerConfig.spoolConfig.fileCleaner,
            dbCleaner));

         if (wsConfig.getContainer().getValueStorages() != null)
         {
            List<File> dataDirsList = initDataDirs();
            List<File> backupDirsList = initBackupDirs(storageDir);

            restorers.add(new DirectoryRestore(dataDirsList, backupDirsList));
         }

         return new ComplexDataRestore(restorers);
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
   }

   private List<File> initBackupDirs(File storageDir) throws RepositoryConfigurationException
   {
      List<File> backupDirsList = new ArrayList<File>();

      for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
      {
         File zipFile = new File(storageDir, "values-" + valueStorage.getId() + ".zip");
         if (PrivilegedFileHelper.exists(zipFile))
         {
            backupDirsList.add(zipFile);
         }
         else
         {
            // try to check if we have deal with old backup format
            zipFile = new File(storageDir, "values/" + valueStorage.getId());
            if (PrivilegedFileHelper.exists(zipFile))
            {
               backupDirsList.add(zipFile);
            }
            else
            {
               throw new RepositoryConfigurationException("There is no backup data for value storage with id "
                  + valueStorage.getId());
            }
         }
      }

      return backupDirsList;
   }

   private List<File> initDataDirs() throws RepositoryConfigurationException
   {
      List<File> dataDirsList = new ArrayList<File>();

      for (ValueStorageEntry valueStorage : wsConfig.getContainer().getValueStorages())
      {
         File dataDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
         dataDirsList.add(dataDir);
      }

      return dataDirsList;
   }

   private DBCleanerTool getDbCleaner(DataRestoreContext context, Connection jdbcConn) throws BackupException
   {
      DBCleanerTool dbCleaner;

      if (context.getObject(DataRestoreContext.DB_CLEANER) != null)
      {
         dbCleaner = (DBCleanerTool)context.getObject(DataRestoreContext.DB_CLEANER);
      }
      else
      {
         try
         {
            dbCleaner = DBCleanService.getWorkspaceDBCleaner(jdbcConn, wsConfig);
         }
         catch (DBCleanException e)
         {
            throw new BackupException(e);
         }
      }
      return dbCleaner;
   }

   private File getStorageDir(DataRestoreContext context)
   {
      return (File)context.getObject(DataRestoreContext.STORAGE_DIR);
   }

   private Connection getJdbcConnection(DataRestoreContext context) throws BackupException
   {
      Connection jdbcConnection = null;

      if (context.getObject(DataRestoreContext.DB_CONNECTION) == null)
      {
         try
         {
            jdbcConnection = connFactory.getJdbcConnection();
            jdbcConnection.setAutoCommit(false);
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
         jdbcConnection = (Connection)context.getObject(DataRestoreContext.DB_CONNECTION);
      }

      return jdbcConnection;
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
      return connFactory.isReindexingSupport();
   }

   /**
    * {@inheritDoc}
    */
   public Long getNodesCount() throws RepositoryException
   {
      WorkspaceStorageConnection conn = connFactory.openConnection();
      try
      {
         return conn.getNodesCount();
      }
      finally
      {
         conn.close();
      }
   }

   /**
    * Get the data source from the InitialContext and wraps it into a {@link ManagedDataSource}
    * in case it has been configured as managed
    */
   protected DataSource getDataSource() throws RepositoryException
   {
      try
      {
         return containerConfig.dsProvider.getDataSource(containerConfig.dbSourceName);
      }
      catch (NamingException e)
      {
         throw new RepositoryException("Datasource '" + containerConfig.dbSourceName
            + "' is not bound in this context.", e);
      }
   }

   /**
    * Validate dialect.
    * 
    * @param confParam
    *          String, dialect from configuration.
    * @return String
    *           return dialect. By default return DB_DIALECT_GENERIC. 
    * 
    */
   private String validateDialect(String confParam)
   {
      for (String dbType : DBConstants.DB_DIALECTS)
      {
         if (confParam.equals(dbType))
         {
            return dbType;
         }
      }

      return DBConstants.DB_DIALECT_AUTO; // by default
   }
}
