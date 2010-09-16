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

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.CompositeChangesLog;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.ItemImpl.ItemType;
import org.exoplatform.services.jcr.impl.core.lock.LockRemover;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedCacheHelper;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.transaction.TransactionService;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.CacheLoader;
import org.jboss.cache.loader.CacheLoaderManager;
import org.jboss.cache.lock.TimeoutException;
import org.picocontainer.Startable;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.Transaction;
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
public class CacheableLockManagerImpl implements CacheableLockManager, ItemsPersistenceListener, Startable
{
   /**
    *  The name to property time out.  
    */
   public static final String TIME_OUT = "time-out";

   /**
    *  The name to property cache configuration. 
    */
   public static final String JBOSSCACCHE_CONFIG = "jbosscache-configuration";

   public static final String JBOSSCACHE_JDBC_CL_DATASOURCE = "jbosscache-cl-cache.jdbc.datasource";

   public static final String JBOSSCACHE_JDBC_CL_NODE_COLUMN = "jbosscache-cl-cache.jdbc.node.type";

   public static final String JBOSSCACHE_JDBC_CL_FQN_COLUMN = "jbosscache-cl-cache.jdbc.fqn.type";

   /**
    * Indicate whether the JBoss Cache instance used can be shared with other caches
    */
   public static final String JBOSSCACHE_SHAREABLE = "jbosscache-shareable";

   public static final Boolean JBOSSCACHE_SHAREABLE_DEFAULT = Boolean.FALSE;

   public static final String JBOSSCACHE_JDBC_CL_AUTO = "auto";

   /**
    * Default lock time out. 30min
    */
   public static final long DEFAULT_LOCK_TIMEOUT = 1000 * 60 * 30;

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

   /**
    * Data manager.
    */
   private final DataManager dataManager;

   /**
    * Run time lock time out.
    */
   private long lockTimeOut;

   /**
    * Lock remover thread.
    */
   private LockRemover lockRemover;

   /**
    * The current Transaction Manager
    */
   private TransactionManager tm;

   private Cache<Serializable, Object> cache;

   private final Fqn<String> lockRoot;
   
   private final boolean shareable;

   /**
    * SessionLockManagers that uses this LockManager.
    */
   private Map<String, CacheableSessionLockManager> sessionLockManagers;

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionService 
    *          the transaction service
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionService transactionService, ConfigurationManager cfm)
      throws RepositoryConfigurationException, RepositoryException
   {
      this(dataManager, config, context, transactionService.getTransactionManager(), cfm);
   }

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, ConfigurationManager cfm) throws RepositoryConfigurationException,
      RepositoryException
   {
      this(dataManager, config, context, (TransactionManager)null, cfm);

   }

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionManager 
    *          the transaction manager
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionManager transactionManager, ConfigurationManager cfm)
      throws RepositoryConfigurationException, RepositoryException
   {
      lockRoot = Fqn.fromElements(config.getUniqueName(), LOCKS);

      List<SimpleParameterEntry> paramenerts = config.getLockManager().getParameters();

      this.dataManager = dataManager;
      if (config.getLockManager() != null)
      {
         if (paramenerts != null && config.getLockManager().getParameterValue(TIME_OUT, null) != null)
         {
            long timeOut = config.getLockManager().getParameterTime(TIME_OUT);
            lockTimeOut = timeOut > 0 ? timeOut : DEFAULT_LOCK_TIMEOUT;
         }
         else
         {
            lockTimeOut =
               config.getLockManager().getTimeout() > 0 ? config.getLockManager().getTimeout() : DEFAULT_LOCK_TIMEOUT;
         }
      }
      else
      {
         lockTimeOut = DEFAULT_LOCK_TIMEOUT;
      }

      sessionLockManagers = new ConcurrentHashMap<String, CacheableSessionLockManager>();

      dataManager.addItemPersistenceListener(this);

      // make cache
      if (config.getLockManager() != null)
      {
         this.tm = transactionManager;

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
         cache = ExoJBossCacheFactory.getUniqueInstance(CacheType.LOCK_CACHE, rootFqn, cache, shareable);
         PrivilegedCacheHelper.create(cache);
         if (cache.getCacheStatus().startAllowed())
         {
            // Add the cache loader needed to prevent TimeoutException
            addCacheLoader();
            PrivilegedCacheHelper.start(cache);
         }

         createStructuredNode(lockRoot);

         // Context recall is a workaround of JDBCCacheLoader starting. 
         context.recall();
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }
   }

   /**
    * If JDBC cache loader is used, then fills-in column types. If column type configured from jcr-configuration file,
    * then nothing is overridden. Parameters are injected into the given parameterEntry.
    */
   public void configureJDBCCacheLoader(MappedParametrizedObjectEntry parameterEntry) throws RepositoryException
   {
      String dataSourceName = parameterEntry.getParameterValue(JBOSSCACHE_JDBC_CL_DATASOURCE, null);
      // if data source is defined, then inject correct data-types.
      // Also it cans be not defined and nothing should be injected (i.e. no cache loader is used (possibly pattern is changed, to used another cache loader))
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
                  jdbcConn = AccessController.doPrivileged(action);
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
         if (dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
         {
            blobType = "OBJECT";
         }
         // MYSQL
         else if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
         {
            blobType = "LONGBLOB";
         }
         // ORACLE
         else if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
         {
            // Oracle suggests the use VARCHAR2 instead of VARCHAR while declaring data type.
            charType = "VARCHAR2(512)";
         }
         // POSTGRE SQL
         else if (dialect.equals(DBConstants.DB_DIALECT_PGSQL))
         {
            blobType = "bytea";
         }
         // Microsoft SQL
         else if (dialect.equals(DBConstants.DB_DIALECT_MSSQL))
         {
            blobType = "VARBINARY(MAX)";
         }
         // SYBASE
         else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
         {
            blobType = "IMAGE";
         }
         // INGRES
         else if (dialect.equals(DBConstants.DB_DIALECT_INGRES))
         {
            blobType = "long byte";
         }
         // else GENERIC, DB2 etc

         // set parameters if not defined
         // if parameter is missing in configuration, then getParameterValue(JBOSSCACHE_JDBC_CL_NODE_COLUMN, JBOSSCACHE_JDBC_CL_AUTO) 
         // will return JBOSSCACHE_JDBC_CL_AUTO. If parameter is present in configuration and equals to "auto", then it should be replaced 
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

   @Managed
   @ManagedDescription("Remove the expired locks")
   public void cleanExpiredLocks()
   {
      removeExpired();
   }

   public long getDefaultLockTimeOut()
   {
      return lockTimeOut;
   }

   private final LockActionNonTxAware<Integer, Object> getNumLocks = new LockActionNonTxAware<Integer, Object>()
   {
      public Integer execute(Object arg)
      {
         return ((CacheSPI<Serializable, Object>)cache).getNode(lockRoot).getChildrenDirect().size();
      }
   };

   @Managed
   @ManagedDescription("The number of active locks")
   public int getNumLocks()
   {
      try
      {
         return executeLockActionNonTxAware(getNumLocks, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return -1;
   }

   private final LockActionNonTxAware<Boolean, Object> hasLocks = new LockActionNonTxAware<Boolean, Object>()
   {
      public Boolean execute(Object arg)
      {
         return ((CacheSPI<Serializable, Object>)cache).getNode(lockRoot).hasChildrenDirect();
      }
   };

   /**
    * Indicates if some locks have already been created
    */
   private boolean hasLocks()
   {
      try
      {
         return executeLockActionNonTxAware(hasLocks, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return true;
   }

   /**
    * Return new instance of session lock manager.
    */
   public SessionLockManager getSessionLockManager(String sessionId, SessionDataManager transientManager)
   {
      CacheableSessionLockManager sessionManager = new CacheableSessionLockManager(sessionId, this, transientManager);
      sessionLockManagers.put(sessionId, sessionManager);
      return sessionManager;
   }

   private final LockActionNonTxAware<Boolean, String> isLockLive = new LockActionNonTxAware<Boolean, String>()
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

   /**
    * Check is LockManager contains lock. No matter it is in pending or persistent state.
    * 
    * @param nodeId - locked node id
    * @return 
    */
   public boolean isLockLive(String nodeId) throws LockException
   {
      try
      {
         return executeLockActionNonTxAware(isLockLive, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /*
    * (non-Javadoc)
    * @seeorg.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.
    * exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      List<PlainChangesLog> chengesLogList = new ArrayList<PlainChangesLog>();
      if (changesLog instanceof TransactionChangesLog)
      {
         ChangesLogIterator logIterator = ((TransactionChangesLog)changesLog).getLogIterator();

         while (logIterator.hasNextLog())
         {
            chengesLogList.add(logIterator.nextLog());
         }
      }
      else if (changesLog instanceof PlainChangesLog)
      {
         chengesLogList.add((PlainChangesLog)changesLog);
      }
      else if (changesLog instanceof CompositeChangesLog)
      {
         for (ChangesLogIterator iter = ((CompositeChangesLog)changesLog).getLogIterator(); iter.hasNextLog();)
         {
            chengesLogList.add(iter.nextLog());
         }
      }

      List<LockOperationContainer> containers = new ArrayList<LockOperationContainer>();

      for (PlainChangesLog currChangesLog : chengesLogList)
      {
         String sessionId = currChangesLog.getSessionId();

         String nodeIdentifier;
         try
         {
            switch (currChangesLog.getEventType())
            {
               case ExtendedEvent.LOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.LOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }
                  nodeIdentifier = currChangesLog.getAllStates().get(0).getData().getParentIdentifier();

                  CacheableSessionLockManager session = sessionLockManagers.get(sessionId);
                  if (session != null && session.containsPendingLock(nodeIdentifier))
                  {
                     containers.add(new LockOperationContainer(nodeIdentifier, currChangesLog.getSessionId(),
                        ExtendedEvent.LOCK));
                  }
                  else
                  {
                     LOG.error("Lock must exist in pending locks.");
                  }
                  break;
               case ExtendedEvent.UNLOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.UNLOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }

                  containers.add(new LockOperationContainer(currChangesLog.getAllStates().get(0).getData()
                     .getParentIdentifier(), currChangesLog.getSessionId(), ExtendedEvent.UNLOCK));
                  break;
               default :
                  HashSet<String> removedLock = new HashSet<String>();
                  for (ItemState itemState : currChangesLog.getAllStates())
                  {
                     // this is a node and node is locked
                     if (itemState.getData().isNode() && lockExist(itemState.getData().getIdentifier()))
                     {
                        nodeIdentifier = itemState.getData().getIdentifier();
                        if (itemState.isDeleted())
                        {
                           removedLock.add(nodeIdentifier);
                        }
                        else if (itemState.isAdded() || itemState.isRenamed() || itemState.isUpdated())
                        {
                           removedLock.remove(nodeIdentifier);
                        }
                     }
                  }
                  for (String identifier : removedLock)
                  {
                     containers.add(new LockOperationContainer(identifier, currChangesLog.getSessionId(),
                        ExtendedEvent.UNLOCK));
                  }
                  break;
            }
         }
         catch (IllegalStateException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }

      // sort locking and unlocking operations to avoid deadlocks in JBossCache
      Collections.sort(containers);
      for (LockOperationContainer container : containers)
      {
         try
         {
            container.apply();
         }
         catch (LockException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }
   }

   /**
    * Class containing operation type (LOCK or UNLOCK) and all the needed information like node uuid and session id.
    */
   private class LockOperationContainer implements Comparable<LockOperationContainer>
   {

      private String identifier;

      private String sessionId;

      private int type;

      /**
       * @param identifier node identifier 
       * @param sessionId id of session
       * @param type ExtendedEvent type specifying the operation (LOCK or UNLOCK)
       */
      public LockOperationContainer(String identifier, String sessionId, int type)
      {
         super();
         this.identifier = identifier;
         this.sessionId = sessionId;
         this.type = type;
      }

      /**
       * @return node identifier
       */
      public String getIdentifier()
      {
         return identifier;
      }

      public void apply() throws LockException
      {
         // invoke internalLock in LOCK operation
         if (type == ExtendedEvent.LOCK)
         {
            internalLock(sessionId, identifier);
         }
         // invoke internalUnLock in UNLOCK operation
         else if (type == ExtendedEvent.UNLOCK)
         {
            internalUnLock(sessionId, identifier);
         }
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      public int compareTo(LockOperationContainer o)
      {
         return identifier.compareTo(o.getIdentifier());
      }
   }

   private final LockActionNonTxAware<Object, LockData> refresh = new LockActionNonTxAware<Object, LockData>()
   {
      public Object execute(LockData newLockData) throws LockException
      {
         Fqn<String> fqn = makeLockFqn(newLockData.getNodeIdentifier());
         Object oldValue = PrivilegedCacheHelper.put(cache, fqn, LOCK_DATA, newLockData);
         if (oldValue == null)
         {
            throw new LockException("Can't refresh lock for node " + newLockData.getNodeIdentifier()
               + " since lock is not exist");
         }
         return null;
      }
   };

   /**
    * Refreshed lock data in cache
    * 
    * @param newLockData
    */
   public void refreshLockData(LockData newLockData) throws LockException
   {
      executeLockActionNonTxAware(refresh, newLockData);
   }

   /**
    * Remove expired locks. Used from LockRemover.
    */
   public synchronized void removeExpired()
   {
      final List<String> removeLockList = new ArrayList<String>();

      for (LockData lock : getLockList())
      {
         if (!lock.isSessionScoped() && lock.getTimeToDeath() < 0)
         {
            removeLockList.add(lock.getNodeIdentifier());
         }
      }

      Collections.sort(removeLockList);

      for (String rLock : removeLockList)
      {
         removeLock(rLock);
      }
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      lockRemover = new LockRemover(this);
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
      lockRemover.halt();
      lockRemover.interrupt();
      sessionLockManagers.clear();
      if (shareable)
      {
         // The cache cannot be stopped since it can be shared so we evict the root node instead
         cache.evict(lockRoot);         
      }
      else
      {
         PrivilegedCacheHelper.stop(cache);         
      }
   }

   /**
    * Copy <code>PropertyData prop<code> to new TransientItemData
    * 
    * @param prop
    * @return
    * @throws RepositoryException
    */
   private TransientItemData copyItemData(PropertyData prop) throws RepositoryException
   {
      if (prop == null)
      {
         return null;
      }

      // make a copy, value may be null for deleting items
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued(), prop.getValues());

      return newData;
   }

   /**
    * Internal lock
    * 
    * @param nodeIdentifier
    * @throws LockException
    */
   private synchronized void internalLock(String sessionId, String nodeIdentifier) throws LockException
   {
      CacheableSessionLockManager session = sessionLockManagers.get(sessionId);
      if (session != null && session.containsPendingLock(nodeIdentifier))
      {
         LockData lockData = session.getPendingLock(nodeIdentifier);
         Fqn<String> lockPath = makeLockFqn(lockData.getNodeIdentifier());

         // addChild will add if absent or return old if present
         Node<Serializable, Object> node = cache.getRoot().addChild(lockPath);

         // this will return null if success. And old data if something exists...
         LockData oldLockData = (LockData)node.putIfAbsent(LOCK_DATA, lockData);

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
    * Internal unlock.
    * 
    * @param sessionId
    * @param nodeIdentifier
    * @throws LockException
    */
   private synchronized void internalUnLock(String sessionId, String nodeIdentifier) throws LockException
   {
      LockData lData = getLockDataById(nodeIdentifier);

      if (lData != null)
      {
         cache.removeNode(makeLockFqn(nodeIdentifier));

         CacheableSessionLockManager sessMgr = sessionLockManagers.get(sessionId);
         if (sessMgr != null)
         {
            sessMgr.notifyLockRemoved(nodeIdentifier);
         }
      }
   }

   private final LockActionNonTxAware<Boolean, String> lockExist = new LockActionNonTxAware<Boolean, String>()
   {
      public Boolean execute(String nodeId) throws LockException
      {
         return cache.get(makeLockFqn(nodeId), LOCK_DATA) != null;
      }
   };

   /**
    * {@inheritDoc}
    */
   public boolean lockExist(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(lockExist, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public String getLockTokenHash(String token)
   {
      String hash = "";
      try
      {
         MessageDigest m = MessageDigest.getInstance("MD5");
         m.update(token.getBytes(), 0, token.length());
         hash = new BigInteger(1, m.digest()).toString(16);
      }
      catch (NoSuchAlgorithmException e)
      {
         LOG.error("Can't get instanse of MD5 MessageDigest!", e);
      }
      return hash;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeOrCloseParentLock(NodeData node) throws RepositoryException
   {
      return getExactNodeOrCloseParentLock(node, true);
   }

   private LockData getExactNodeOrCloseParentLock(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;
      retval = getLockDataById(node.getIdentifier());
      if (retval == null)
      {
         NodeData parentData = (NodeData)dataManager.getItemData(node.getParentIdentifier());
         if (parentData != null)
         {
            retval = getExactNodeOrCloseParentLock(parentData, false);
         }
      }
      return retval;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeLock(NodeData node) throws RepositoryException
   {
      if (node == null || !hasLocks())
      {
         return null;
      }

      return getLockDataById(node.getIdentifier());
   }

   /**
    * {@inheritDoc}
    */
   public LockData getClosedChild(NodeData node) throws RepositoryException
   {
      return getClosedChild(node, true);
   }

   private LockData getClosedChild(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;

      List<NodeData> childData = dataManager.getChildNodesData(node);
      for (NodeData nodeData : childData)
      {
         retval = getLockDataById(nodeData.getIdentifier());
         if (retval != null)
            return retval;
      }
      // child not found try to find dipper
      for (NodeData nodeData : childData)
      {
         retval = getClosedChild(nodeData, false);
         if (retval != null)
            return retval;
      }
      return retval;
   }

   private final LockActionNonTxAware<LockData, String> getLockDataById = new LockActionNonTxAware<LockData, String>()
   {
      public LockData execute(String nodeId) throws LockException
      {
         return (LockData)cache.get(makeLockFqn(nodeId), LOCK_DATA);
      }
   };

   protected LockData getLockDataById(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(getLockDataById, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   private final LockActionNonTxAware<List<LockData>, Object> getLockList =
      new LockActionNonTxAware<List<LockData>, Object>()
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

   protected synchronized List<LockData> getLockList()
   {
      try
      {
         return executeLockActionNonTxAware(getLockList, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   /**
    * Remove lock, used by Lock remover.
    * 
    * @param nodeIdentifier String
    */
   protected void removeLock(String nodeIdentifier)
   {
      try
      {
         NodeData nData = (NodeData)dataManager.getItemData(nodeIdentifier);

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that node was removed in other node of cluster.  
         if (nData == null)
         {
            return;
         }

         PlainChangesLog changesLog =
            new PlainChangesLogImpl(new ArrayList<ItemState>(), SystemIdentity.SYSTEM, ExtendedEvent.UNLOCK);

         ItemData lockOwner =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKOWNER, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockOwner == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockOwner));

         ItemData lockIsDeep =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKISDEEP, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockIsDeep == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockIsDeep));

         // lock probably removed by other thread
         if (lockOwner == null && lockIsDeep == null)
         {
            return;
         }

         dataManager.save(new TransactionChangesLog(changesLog));
      }
      catch (JCRInvalidItemStateException e)
      {
         //TODO EXOJCR-412, should be refactored in future.
         //Skip property not found in DB, because that lock property was removed in other node of cluster.
         if (LOG.isDebugEnabled())
         {
            LOG.debug("The propperty was removed in other node of cluster.", e);
         }

      }
      catch (RepositoryException e)
      {
         LOG.error("Error occur during removing lock" + e.getLocalizedMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void closeSessionLockManager(String sessionID)
   {
      sessionLockManagers.remove(sessionID);
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
   private void createStructuredNode(Fqn<String> fqn)
   {
      Node<Serializable, Object> node = cache.getRoot().getChild(fqn);
      if (node == null)
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         node = cache.getRoot().addChild(fqn);
      }
      node.setResident(true);
   }

   /**
    * Execute the given action outside a transaction. This is needed since the {@link Cache} used by {@link CacheableLockManagerImpl}
    * manages the persistence of its locks thanks to a {@link CacheLoader} and a {@link CacheLoader} lock the JBoss cache {@link Node}
    * even for read operations which cause deadlock issue when a XA {@link Transaction} is already opened
    * @throws LockException when a exception occurs
    */
   private <R, A> R executeLockActionNonTxAware(LockActionNonTxAware<R, A> action, A arg) throws LockException
   {
      Transaction tx = null;
      try
      {
         if (tm != null)
         {
            try
            {
               tx = tm.suspend();
            }
            catch (Exception e)
            {
               LOG.warn("Cannot suspend the current transaction", e);
            }
         }
         return action.execute(arg);
      }
      finally
      {
         if (tx != null)
         {
            try
            {
               tm.resume(tx);
            }
            catch (Exception e)
            {
               LOG.warn("Cannot resume the current transaction", e);
            }
         }
      }
   }

   /**
    * Actions that are not supposed to be called within a transaction
    * 
    * Created by The eXo Platform SAS
    * Author : Nicolas Filotto 
    *          nicolas.filotto@exoplatform.com
    * 21 janv. 2010
    */
   private static interface LockActionNonTxAware<R, A>
   {
      R execute(A arg) throws LockException;
   }
}
