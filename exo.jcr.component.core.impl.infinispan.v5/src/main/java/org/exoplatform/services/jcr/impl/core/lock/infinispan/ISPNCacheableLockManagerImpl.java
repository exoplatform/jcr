/*
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
package org.exoplatform.services.jcr.impl.core.lock.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.CacheableSessionLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.LockData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.transaction.TransactionService;
import org.infinispan.Cache;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.lifecycle.ComponentStatus;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: InfinispanLockManagerImpl.java 111 2010-11-11 11:11:11Z tolusha $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "lockmanager"))
public class ISPNCacheableLockManagerImpl extends AbstractCacheableLockManager
{

   /**
    *  The name to property cache configuration. 
    */
   public static final String INFINISPAN_JDBC_CL_DATASOURCE = "infinispan-cl-cache.jdbc.datasource";

   public static final String INFINISPAN_JDBC_CL_DATA_COLUMN = "infinispan-cl-cache.jdbc.data.type";

   public static final String INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN = "infinispan-cl-cache.jdbc.timestamp.type";

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN = "infinispan-cl-cache.jdbc.id.type";

   public static final String INFINISPAN_JDBC_TABLE_NAME = "infinispan-cl-cache.jdbc.table.name";

   public static final String INFINISPAN_JDBC_CL_AUTO = "auto";

   /**
    * Logger
    */
   private final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.InfinispanLockManagerImpl");

   private Cache<Serializable, Object> cache;

   public ISPNCacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionService transactionService, ConfigurationManager cfm,
      LockRemoverHolder lockRemoverHolder) throws RepositoryConfigurationException, RepositoryException
   {
      this(dataManager, config, context, transactionService.getTransactionManager(), cfm, lockRemoverHolder);
   }

   public ISPNCacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, ConfigurationManager cfm, LockRemoverHolder lockRemoverHolder)
      throws RepositoryConfigurationException, RepositoryException
   {
      this(dataManager, config, context, (TransactionManager)null, cfm, lockRemoverHolder);
   }

   public ISPNCacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionManager transactionManager, ConfigurationManager cfm,
      LockRemoverHolder lockRemoverHolder) throws RepositoryConfigurationException, RepositoryException
   {
      super(dataManager, config, transactionManager, lockRemoverHolder);

      // make cache
      if (config.getLockManager() != null)
      {
         // create cache using custom factory
         ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm);

         // configure cache loader parameters with correct DB data-types
         configureJDBCCacheLoader(config.getLockManager());

         cache = factory.createCache("L" + config.getUniqueName().replace("_", ""), config.getLockManager());
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }

      this.getNumLocks = new LockActionNonTxAware<Integer, Object>()
      {
         public Integer execute(Object arg)
         {
            return cache.size();
         }
      };

      this.hasLocks = new LockActionNonTxAware<Boolean, Object>()
      {
         public Boolean execute(Object arg)
         {
            return !cache.isEmpty();
         }
      };

      this.isLockLive = new LockActionNonTxAware<Boolean, String>()
      {
         public Boolean execute(String nodeId)
         {
            return cache.containsKey(nodeId);
         }
      };

      this.refresh = new LockActionNonTxAware<Object, LockData>()
      {
         public Object execute(LockData newLockData) throws LockException
         {
            Object oldValue = PrivilegedISPNCacheHelper.put(cache, newLockData.getNodeIdentifier(), newLockData);
            if (oldValue == null)
            {
               throw new LockException("Can't refresh lock for node " + newLockData.getNodeIdentifier()
                  + " since lock is not exist");
            }
            return null;
         }
      };

      this.lockExist = new LockActionNonTxAware<Boolean, String>()
      {
         public Boolean execute(String nodeId) throws LockException
         {
            return cache.containsKey(nodeId);
         }
      };

      this.getLockDataById = new LockActionNonTxAware<LockData, String>()
      {
         public LockData execute(String nodeId) throws LockException
         {
            return (LockData)cache.get(nodeId);
         }
      };

      this.getLockList = new LockActionNonTxAware<List<LockData>, Object>()
      {
         public List<LockData> execute(Object arg) throws LockException
         {
            Collection<Object> datas = cache.values();

            List<LockData> locksData = new ArrayList<LockData>();
            for (Object lockData : datas)
            {
               if (lockData != null)
               {
                  locksData.add((LockData)lockData);
               }
            }
            return locksData;
         }
      };
   }

   /**
    * If JDBC cache loader is used, then fills-in column types. If column type configured from jcr-configuration file,
    * then nothing is overridden. Parameters are injected into the given parameterEntry.
    */
   private void configureJDBCCacheLoader(MappedParametrizedObjectEntry parameterEntry) throws RepositoryException
   {
      String dataSourceName = parameterEntry.getParameterValue(INFINISPAN_JDBC_CL_DATASOURCE, null);
      // if data source is defined, then inject correct data-types.
      // Also it cans be not defined and nothing should be injected 
      //(i.e. no cache loader is used (possibly pattern is changed, to used another cache loader))
      if (dataSourceName != null)
      {
         String dialect;
         // detect dialect of data-source
         try
         {
            final DataSource dataSource = (DataSource)new InitialContext().lookup(dataSourceName);
            if (dataSource == null)
            {
               throw new RepositoryException("DataSource (" + dataSourceName + ") can't be null");
            }

            Connection jdbcConn = null;
            try
            {
               PrivilegedExceptionAction<Connection> action = new PrivilegedExceptionAction<Connection>()
               {
                  public Connection run() throws Exception
                  {
                     return dataSource.getConnection();
                  }
               };
               try
               {
                  jdbcConn = SecurityHelper.doPrivilegedExceptionAction(action);
               }
               catch (PrivilegedActionException pae)
               {
                  Throwable cause = pae.getCause();
                  if (cause instanceof SQLException)
                  {
                     throw (SQLException)cause;
                  }
                  else if (cause instanceof RuntimeException)
                  {
                     throw (RuntimeException)cause;
                  }
                  else
                  {
                     throw new RuntimeException(cause);
                  }
               }

               dialect = DialectDetecter.detect(jdbcConn.getMetaData());
            }
            finally
            {
               if (jdbcConn != null && !jdbcConn.isClosed())
               {
                  try
                  {
                     jdbcConn.close();
                  }
                  catch (SQLException e)
                  {
                     throw new RepositoryException("Error of connection close", e);
                  }
               }
            }
         }
         catch (Exception e)
         {
            throw new RepositoryException("Error configuring JDBC cache loader", e);
         }

         // default values, will be overridden with types suitable for concrete data base.
         String blobType = "BLOB";
         String charType = "VARCHAR(512)";
         String timeStampType = "BIGINT";
         // HSSQL
         if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_HSQLDB))
         {
            blobType = "VARBINARY(65535)";
         }
         // MYSQL
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL)
            || dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_UTF8)
            || dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_MYISAM)
            || dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_MYISAM_UTF8))
         {
            blobType = "LONGBLOB";
         }
         // ORACLE
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLE)
            || dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLEOCI))
         {
            // Oracle suggests the use VARCHAR2 instead of VARCHAR while declaring data type.
            charType = "VARCHAR2(512)";
            timeStampType = "NUMBER(19, 0)";
         }
         // POSTGRE SQL
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_PGSQL))
         {
            blobType = "bytea";
         }
         // Microsoft SQL
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MSSQL))
         {
            blobType = "VARBINARY(MAX)";
         }
         // SYBASE
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_SYBASE))
         {
            blobType = "IMAGE";
         }
         // INGRES
         else if (dialect.equalsIgnoreCase(DBConstants.DB_DIALECT_INGRES))
         {
            blobType = "long byte";
         }
         // else GENERIC, DB2 etc

         // set parameters if not defined
         // if parameter is missing in configuration, then 
         // getParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN, INFINISPAN_JDBC_CL_AUTO) 
         // will return INFINISPAN_JDBC_CL_AUTO. If parameter is present in configuration and 
         //equals to "auto", then it should be replaced 
         // with correct value for given database
         if (parameterEntry.getParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN, INFINISPAN_JDBC_CL_AUTO)
            .equalsIgnoreCase(INFINISPAN_JDBC_CL_AUTO))
         {
            parameterEntry.putParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN, blobType);
         }

         if (parameterEntry.getParameterValue(INFINISPAN_JDBC_CL_ID_COLUMN, INFINISPAN_JDBC_CL_AUTO).equalsIgnoreCase(
            INFINISPAN_JDBC_CL_AUTO))
         {
            parameterEntry.putParameterValue(INFINISPAN_JDBC_CL_ID_COLUMN, charType);
         }

         if (parameterEntry.getParameterValue(INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN, INFINISPAN_JDBC_CL_AUTO)
            .equalsIgnoreCase(INFINISPAN_JDBC_CL_AUTO))
         {
            parameterEntry.putParameterValue(INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN, timeStampType);
         }
      }
      else
      {
         LOG.warn("CacheLoader DataSource " + INFINISPAN_JDBC_CL_DATASOURCE + " is not configured.");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void stop()
   {
      super.stop();
      PrivilegedISPNCacheHelper.stop(cache);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected synchronized void internalLock(String sessionId, String nodeIdentifier) throws LockException
   {
      CacheableSessionLockManager session = sessionLockManagers.get(sessionId);
      if (session != null && session.containsPendingLock(nodeIdentifier))
      {
         LockData lockData = session.getPendingLock(nodeIdentifier);

         // this will return null if success. And old data if something exists...
         LockData oldLockData = doPut(lockData);

         if (oldLockData != null)
         {
            throw new LockException("Unable to write LockData. Node [" + lockData.getNodeIdentifier()
               + "] already has LockData!");
         }

         session.notifyLockPersisted(nodeIdentifier);
      }
      else
      {
         throw new LockException("No lock in pending locks");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected synchronized void internalUnLock(String sessionId, String nodeIdentifier) throws LockException
   {
      LockData lData = getLockDataById(nodeIdentifier);

      if (lData != null)
      {
         doRemove(lData);

         CacheableSessionLockManager sessMgr = sessionLockManagers.get(sessionId);
         if (sessMgr != null)
         {
            sessMgr.notifyLockRemoved(nodeIdentifier);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected LockData doPut(LockData lockData)
   {
      return (LockData)PrivilegedISPNCacheHelper.putIfAbsent(cache, lockData.getNodeIdentifier(), lockData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doRemove(LockData lockData)
   {
      cache.remove(lockData.getNodeIdentifier());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isAloneInCluster()
   {
      return cache.getConfiguration().getCacheMode() == CacheMode.LOCAL
         || cache.getCacheManager().getMembers().size() == 1;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doClean()
   {
      if (cache.getStatus() == ComponentStatus.RUNNING)
      {
         for (LockData lockData : getLockList())
         {
            doRemove(lockData);
         }
      }
   }
}
