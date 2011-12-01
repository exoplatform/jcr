/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainerContext;
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
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.exoplatform.services.jcr.jbosscache.PrivilegedJBossCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.transaction.TransactionService;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.config.Configuration.CacheMode;
import org.jboss.cache.jmx.JmxRegistrationManager;
import org.jboss.cache.loader.CacheLoader;
import org.jboss.cache.loader.CacheLoaderManager;
import org.jboss.cache.lock.TimeoutException;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CacheableLockManager.java 111 2008-11-11 11:11:11Z serg $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "lockmanager"))
public class CacheableLockManagerImpl extends AbstractCacheableLockManager
{

   public static final String JBOSSCACHE_JDBC_CL_DATASOURCE = "jbosscache-cl-cache.jdbc.datasource";

   public static final String JBOSSCACHE_JDBC_CL_NODE_COLUMN = "jbosscache-cl-cache.jdbc.node.type";

   public static final String JBOSSCACHE_JDBC_CL_FQN_COLUMN = "jbosscache-cl-cache.jdbc.fqn.type";

   public static final String JBOSSCACHE_JDBC_TABLE_NAME = "jbosscache-cl-cache.jdbc.table.name";

   /**
    * Indicate whether the JBoss Cache instance used can be shared with other caches
    */
   public static final String JBOSSCACHE_SHAREABLE = "jbosscache-shareable";

   public static final Boolean JBOSSCACHE_SHAREABLE_DEFAULT = Boolean.FALSE;

   public static final String JBOSSCACHE_JDBC_CL_AUTO = "auto";

   /**
    * Name of lock root in jboss-cache.
    */
   public static final String LOCKS = "$LOCKS";

   /**
    * Attribute name where LockData will be stored.
    */
   public static final String LOCK_DATA = "$LOCK_DATA";

   /**
    * Logger
    */
   private final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CacheableLockManagerImpl");

   private Cache<Serializable, Object> cache;

   private final Fqn<String> lockRoot;

   private final boolean shareable;

   private final JmxRegistrationManager jmxManager;
   
   /**
    * Constructor.
    * 
    * @param ctx The container context
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionService 
    *          the transaction service
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(ExoContainerContext ctx, WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionService transactionService, ConfigurationManager cfm,
      LockRemoverHolder lockRemoverHolder) throws RepositoryConfigurationException, RepositoryException
   {
      this(ctx, dataManager, config, context, transactionService.getTransactionManager(), cfm, lockRemoverHolder);
   }

   /**
    * Constructor.
    * 
    * @param ctx The container context
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(ExoContainerContext ctx, WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, ConfigurationManager cfm, LockRemoverHolder lockRemoverHolder)
      throws RepositoryConfigurationException, RepositoryException
   {
      this(ctx, dataManager, config, context, (TransactionManager)null, cfm, lockRemoverHolder);
   }

   /**
    * Constructor.
    * 
    * @param ctx The container context
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionManager 
    *          the transaction manager
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(ExoContainerContext ctx, WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionManager transactionManager, ConfigurationManager cfm,
      LockRemoverHolder lockRemoverHolder) throws RepositoryConfigurationException, RepositoryException
   {
      super(dataManager, config, transactionManager, lockRemoverHolder);

      // make cache
      if (config.getLockManager() != null)
      {
         // create cache using custom factory
         ExoJBossCacheFactory<Serializable, Object> factory =
            new ExoJBossCacheFactory<Serializable, Object>(cfm, transactionManager);

         // configure cache loader parameters with correct DB data-types
         configureJDBCCacheLoader(config.getLockManager());

         cache = factory.createCache(config.getLockManager());
        
         Fqn<String> rootFqn = Fqn.fromElements(config.getUniqueName());

         shareable =
            config.getLockManager().getParameterBoolean(JBOSSCACHE_SHAREABLE, JBOSSCACHE_SHAREABLE_DEFAULT)
               .booleanValue();
         lockRoot = shareable ? Fqn.fromRelativeElements(rootFqn, LOCKS) : Fqn.fromElements(LOCKS);

         cache = ExoJBossCacheFactory.getUniqueInstance(CacheType.LOCK_CACHE, rootFqn, cache, shareable);
         this.jmxManager = ExoJBossCacheFactory.getJmxRegistrationManager(ctx, cache, CacheType.LOCK_CACHE);
         if (jmxManager != null)
         {
            SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
            {
               public Void run()
               {
                  jmxManager.registerAllMBeans();
                  return null;
               }
            });
         }
         PrivilegedJBossCacheHelper.create(cache);
         if (cache.getCacheStatus().startAllowed())
         {
            // Add the cache loader needed to prevent TimeoutException
            addCacheLoader();
            PrivilegedJBossCacheHelper.start(cache);
         }

         createStructuredNode(lockRoot);
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }

      this.getNumLocks = new LockActionNonTxAware<Integer, Object>()
      {
         public Integer execute(Object arg)
         {
            return ((CacheSPI<Serializable, Object>)cache).getNumberOfNodes() - 1;
         }
      };

      this.hasLocks = new LockActionNonTxAware<Boolean, Object>()
      {
         public Boolean execute(Object arg)
         {
            return ((CacheSPI<Serializable, Object>)cache).getNode(lockRoot).hasChildrenDirect();
         }
      };

      this.isLockLive = new LockActionNonTxAware<Boolean, String>()
      {
         public Boolean execute(String nodeId)
         {
            if (cache.get(makeLockFqn(nodeId), LOCK_DATA) != null) //pendingLocks.containsKey(nodeId) || 
            {
               return true;
            }

            return false;
         }
      };

      this.refresh = new LockActionNonTxAware<Object, LockData>()
      {
         public Object execute(LockData newLockData) throws LockException
         {
            Fqn<String> fqn = makeLockFqn(newLockData.getNodeIdentifier());
            Object oldValue = PrivilegedJBossCacheHelper.put(cache, fqn, LOCK_DATA, newLockData);
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
            return cache.get(makeLockFqn(nodeId), LOCK_DATA) != null;
         }
      };

      this.getLockDataById = new LockActionNonTxAware<LockData, String>()
      {
         public LockData execute(String nodeId) throws LockException
         {
            return (LockData)cache.get(makeLockFqn(nodeId), LOCK_DATA);
         }
      };

      this.getLockList = new LockActionNonTxAware<List<LockData>, Object>()
      {
         public List<LockData> execute(Object arg) throws LockException
         {
            Set<Object> nodesId = cache.getChildrenNames(lockRoot);

            List<LockData> locksData = new ArrayList<LockData>();
            for (Object nodeId : nodesId)
            {
               LockData lockData = (LockData)cache.get(makeLockFqn((String)nodeId), LOCK_DATA);
               if (lockData != null)
               {
                  locksData.add(lockData);
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
      String dataSourceName = parameterEntry.getParameterValue(JBOSSCACHE_JDBC_CL_DATASOURCE, null);
      // if data source is defined, then inject correct data-types.
      // Also it cans be not defined and nothing should be injected 
      // (i.e. no cache loader is used (possibly pattern is changed, to used another cache loader))
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
         // getParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN, JBOSSCACHE_JDBC_CL_AUTO) 
         // will return JBOSSCACHE_JDBC_CL_AUTO. If parameter is present in configuration and 
         // equals to "auto", then it should be replaced 
         // with correct value for given database
         if (parameterEntry.getParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN, JBOSSCACHE_JDBC_CL_AUTO)
            .equalsIgnoreCase(JBOSSCACHE_JDBC_CL_AUTO))
         {
            parameterEntry.putParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN, blobType);
         }

         if (parameterEntry.getParameterValue(JBOSSCACHE_JDBC_CL_FQN_COLUMN, JBOSSCACHE_JDBC_CL_AUTO).equalsIgnoreCase(
            JBOSSCACHE_JDBC_CL_AUTO))
         {
            parameterEntry.putParameterValue(JBOSSCACHE_JDBC_CL_FQN_COLUMN, charType);
         }
      }
      else
      {
         LOG.warn("CacheLoader DataSource " + JBOSSCACHE_JDBC_CL_DATASOURCE + " is not configured.");
      }
   }

   /**
    * This methods adds programmatically the required {@link CacheLoader} needed to prevent 
    * any {@link TimeoutException}
    */
   private void addCacheLoader()
   {
      CacheLoaderConfig config = cache.getConfiguration().getCacheLoaderConfig();
      List<IndividualCacheLoaderConfig> oldConfigs;
      if (config == null || (oldConfigs = config.getIndividualCacheLoaderConfigs()) == null || oldConfigs.isEmpty())
      {
         if (LOG.isInfoEnabled())
         {
            LOG.info("No cache loader has been defined, thus no need to encapsulate any cache loader.");
         }
         return;
      }
      CacheLoaderManager clm =
         ((CacheSPI<Serializable, Object>)cache).getComponentRegistry().getComponent(CacheLoaderManager.class);
      if (clm == null)
      {
         LOG.error("The CacheLoaderManager cannot be found");
         return;
      }
      CacheLoader currentCL = clm.getCacheLoader();
      if (currentCL == null)
      {
         LOG.error("The CacheLoader cannot be found");
         return;
      }

      ControllerCacheLoader ccl = new ControllerCacheLoader(currentCL);
      List<IndividualCacheLoaderConfig> newConfig = new ArrayList<IndividualCacheLoaderConfig>(1);
      // create CacheLoaderConfig
      IndividualCacheLoaderConfig cclConfig = new IndividualCacheLoaderConfig();
      // set CacheLoader
      cclConfig.setCacheLoader(ccl);
      // set parameters
      cclConfig.setFetchPersistentState(clm.isFetchPersistentState());
      cclConfig.setAsync(false);
      cclConfig.setIgnoreModifications(false);
      CacheLoaderConfig.IndividualCacheLoaderConfig first = config.getFirstCacheLoaderConfig();
      cclConfig.setPurgeOnStartup(first != null && first.isPurgeOnStartup());
      newConfig.add(cclConfig);
      config.setIndividualCacheLoaderConfigs(newConfig);

      if (LOG.isInfoEnabled())
      {
         LOG.info("The configured cache loader has been encapsulated successfully");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void stop()
   {
      super.stop();
      if (shareable)
      {
         // The cache cannot be stopped since it can be shared so we evict the root node instead
         cache.evict(lockRoot);
      }
      else
      {
         PrivilegedJBossCacheHelper.stop(cache);
      }
      if (jmxManager != null)
      {
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               jmxManager.unregisterAllMBeans();
               return null;
            }
         });
      }      
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
    * Make lock absolute Fqn, i.e. /$LOCKS/nodeID.
    *
    * @param itemId String
    * @return Fqn
    */
   private Fqn<String> makeLockFqn(String nodeId)
   {
      return Fqn.fromRelativeElements(lockRoot, nodeId);
   }

   /**
    *  Will be created structured node in cache, like /$LOCKS
    */
   private void createStructuredNode(final Fqn<String> fqn)
   {
      Node<Serializable, Object> node = cache.getRoot().getChild(fqn);
      if (node == null)
      {
         PrivilegedAction<Node<Serializable, Object>> action = new PrivilegedAction<Node<Serializable, Object>>()
         {
            public Node<Serializable, Object> run()
            {
               cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
               return cache.getRoot().addChild(fqn);
            }
         };
         node = SecurityHelper.doPrivilegedAction(action);
      }
      node.setResident(true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected LockData doPut(LockData lockData)
   {
      Fqn<String> lockPath = makeLockFqn(lockData.getNodeIdentifier());

      // addChild will add if absent or return old if present
      Node<Serializable, Object> node = cache.getRoot().addChild(lockPath);

      // this will return null if success. And old data if something exists...
      return (LockData)node.putIfAbsent(LOCK_DATA, lockData);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void doRemove(LockData lockData)
   {
      PrivilegedJBossCacheHelper.removeNode(cache, makeLockFqn(lockData.getNodeIdentifier()));
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isAloneInCluster()
   {
      return cache.getConfiguration().getCacheMode() == CacheMode.LOCAL || cache.getMembers().size() == 1;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doClean()
   {
      if (cache.getCacheStatus() == CacheStatus.STARTED)
      {
         for (LockData lockData : getLockList())
         {
            doRemove(lockData);
         }
      }
   }
}
