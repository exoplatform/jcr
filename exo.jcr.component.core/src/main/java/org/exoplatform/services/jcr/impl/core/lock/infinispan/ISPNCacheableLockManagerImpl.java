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

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandler;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.CacheableSessionLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.LockData;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.transaction.TransactionService;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.IllegalLifecycleStateException;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.transaction.TransactionManager;

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

   public static final String INFINISPAN_JDBC_CL_DIALECT = "infinispan-cl-cache.jdbc.dialect";

   public static final String INFINISPAN_JDBC_CL_DATA_COLUMN = "infinispan-cl-cache.jdbc.data.type";

   public static final String INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN = "infinispan-cl-cache.jdbc.timestamp.type";

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN = "infinispan-cl-cache.jdbc.id.type";

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN_NAME = "infinispan-cl-cache.jdbc.id.column";

   public static final String INFINISPAN_JDBC_TABLE_NAME = "infinispan-cl-cache.jdbc.table.name";

   public static final String INFINISPAN_JDBC_CL_AUTO = "auto";

   /**
    * The lock store, which starts with the lock manager, once the lock store of Infinispan 8 is upgraded.
    */
   private volatile AdvancedCache<Serializable, Object> cache;

   private AtomicInteger sizeCount = new AtomicInteger(0);

   private ISPNCacheFactory<Serializable, Object> factory;

   /**
    * The upgrade of the lock store of Infinispan 8, null when the lock store has no JDBC store.
    */
   private ISPNLockUpgrade upgrade;

   /**
    * The table of the JDBC store of the locks, null when the lock store has no JDBC store.
    */
   private TableManipulationConfiguration lockTable;

   private volatile boolean stopped;

   /**
    * The lock manager of a workspace with a search manager, which starts first: the upgrade of the lock store, when
    * the lock manager starts, updates the index.
    */
   public ISPNCacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionService transactionService, ConfigurationManager cfm,
      LockRemoverHolder lockRemoverHolder, SearchManager searchManager)
      throws RepositoryConfigurationException, RepositoryException
   {
      this(dataManager, config, context, transactionService, cfm, lockRemoverHolder);
   }

   /**
    * The lock manager of a workspace with a search manager, which starts first: the upgrade of the lock store, when
    * the lock manager starts, updates the index.
    */
   public ISPNCacheableLockManagerImpl(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, ConfigurationManager cfm, LockRemoverHolder lockRemoverHolder,
      SearchManager searchManager) throws RepositoryConfigurationException, RepositoryException
   {
      this(dataManager, config, context, cfm, lockRemoverHolder);
   }

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
         factory = new ISPNCacheFactory<Serializable, Object>(cfm, transactionManager);

         try
         {
            String dataSourceName = config.getLockManager().getParameterValue(INFINISPAN_JDBC_CL_DATASOURCE);
            dataSource = (DataSource)new InitialContext().lookup(dataSourceName);
         }
         catch (NamingException e)
         {
            throw new RepositoryException(e.getMessage(), e);
         }

         // configure cache loader parameters with correct DB data-types
         ISPNCacheFactory.configureCacheStore(config.getLockManager(), INFINISPAN_JDBC_CL_DATASOURCE,
            INFINISPAN_JDBC_CL_DATA_COLUMN, INFINISPAN_JDBC_CL_ID_COLUMN, INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN
            , INFINISPAN_JDBC_CL_DIALECT);
         JdbcStringBasedStoreConfigurationBuilder jdbcStore =
            getJdbcStore(factory.readConfiguration(config.getLockManager()));
         lockTable = jdbcStore == null ? null : jdbcStore.table().create();

         //force clean lock cache entry store
         boolean deleteLocks =
                 "true".equalsIgnoreCase(System.getProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE,
                         "false"));
         if(deleteLocks){
            ISPNLockTableHandler lockTableHandler = (ISPNLockTableHandler) getLockTableHandler();
            try
            {
               if(lockTableHandler.tableExists())
               {
                  lockTableHandler.cleanLocks();
               }
            }
            catch (SQLException e)
            {
               throw new RepositoryException(e.getMessage(), e);
            }
         }

         upgrade = jdbcStore == null ? null : createUpgrade(jdbcStore);
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }

      this.getNumLocks = new LockActionNonTxAware<Integer, Object>()
      {
         public Integer execute(Object arg)
         {
            return sizeCount.get();
         }
      };

      this.hasLocks = new LockActionNonTxAware<Boolean, Object>()
      {
         public Boolean execute(Object arg)
         {
            return (sizeCount.get() > 0);
         }
      };

      this.isLockLive = new LockActionNonTxAware<Boolean, String>()
      {
         public Boolean execute(String nodeId)
         {
            return lockStore().withFlags(Flag.SKIP_CACHE_LOAD).containsKey(nodeId);
         }
      };

      this.refresh = new LockActionNonTxAware<Object, LockData>()
      {
         public Object execute(LockData newLockData) throws LockException
         {
            Object oldValue = PrivilegedISPNCacheHelper.put(lockStore(), newLockData.getNodeIdentifier(), newLockData);
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
            return lockStore().withFlags(Flag.SKIP_CACHE_LOAD).containsKey(nodeId);
         }
      };

      this.getLockDataById = new LockActionNonTxAware<LockData, String>()
      {
         public LockData execute(String nodeId) throws LockException
         {
            return (LockData)lockStore().withFlags(Flag.SKIP_CACHE_LOAD).get(nodeId);
         }
      };

      this.getLockList = new LockActionNonTxAware<List<LockData>, Object>()
      {
         public List<LockData> execute(Object arg) throws LockException
         {
            Collection<Object> datas = lockStore().withFlags(Flag.SKIP_CACHE_LOAD).values();

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
    * {@inheritDoc}
    */
   public void start()
   {
      stopped = false;
      lockStore();
      super.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      // First, and outside the monitor, which a start waiting for another node's upgrade of the lock store holds
      stopped = true;
      super.stop();
      synchronized (this)
      {
         AdvancedCache<Serializable, Object> lockCache = cache;
         if (lockCache != null)
         {
            cache = null;
            PrivilegedISPNCacheHelper.stop(lockCache);
            ISPNCacheFactory.releaseUniqueInstance(lockCache.getCacheManager());
         }
      }
   }

   /**
    * Gives the lock store, created once the lock store of Infinispan 8 is upgraded: when the lock manager starts, or
    * before, when the restore of a workspace replaces its locks.
    */
   private AdvancedCache<Serializable, Object> lockStore()
   {
      AdvancedCache<Serializable, Object> lockCache = cache;
      if (lockCache != null)
      {
         return lockCache;
      }
      synchronized (this)
      {
         if (cache == null)
         {
            if (stopped)
            {
               // The exception of a stopped lock store, which its callers expect
               throw new IllegalLifecycleStateException("The lock manager of the workspace " + config.getUniqueName()
                  + " is stopped");
            }
            cache = createLockStore();
         }
         return cache;
      }
   }

   private AdvancedCache<Serializable, Object> createLockStore()
   {
      AdvancedCache<Serializable, Object> lockCache;
      try
      {
         if (upgrade != null)
         {
            upgrade.run();
         }
         lockCache = factory.createCache("L" + config.getUniqueName().replace("_", ""), config.getLockManager())
                            .getAdvancedCache();
      }
      catch (RepositoryException | RepositoryConfigurationException e)
      {
         if (stopped)
         {
            throw new IllegalLifecycleStateException("The lock manager of the workspace " + config.getUniqueName()
               + " is stopped", e);
         }
         throw new IllegalStateException("The lock store of the workspace " + config.getUniqueName()
            + " cannot start", e);
      }
      lockCache.addListener(new CacheSizeEventListener());
      int size = lockCache.size();
      if (size != 0)
      {
         sizeCount.addAndGet(size);
      }
      PrivilegedISPNCacheHelper.start(lockCache);
      return lockCache;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      // No lock exists before the lock store is created: the changes of the upgrade release those of Infinispan 8
      if (cache != null)
      {
         super.onSaveItems(changesLog);
      }
   }

   /**
    * Gives the upgrade of the lock store of Infinispan 8, after checking that the JDBC store of the lock configuration
    * is segmented: a table without segment column would be taken for the one of Infinispan 8, and its locks released
    * at each start.
    */
   private ISPNLockUpgrade createUpgrade(JdbcStringBasedStoreConfigurationBuilder jdbcStore)
      throws RepositoryConfigurationException, RepositoryException
   {
      if (!jdbcStore.attributes().attribute(AbstractStoreConfiguration.SEGMENTED).get())
      {
         throw new RepositoryConfigurationException("The JDBC store of the lock configuration "
            + config.getLockManager().getParameterValue(ISPNCacheFactory.INFINISPAN_CONFIG)
            + " must be segmented: the lock manager takes a table without segment column for the one of"
            + " Infinispan 8, and releases its locks at each start");
      }
      String itemSourceName = config.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
      try
      {
         return new ISPNLockUpgrade(config, dataSource, (DataSource)new InitialContext().lookup(itemSourceName),
            lockTable, this::releaseLocks, () -> stopped);
      }
      catch (NamingException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   /**
    * @return the JDBC store of the lock configuration, <code>null</code> when it has none
    */
   private static JdbcStringBasedStoreConfigurationBuilder getJdbcStore(ConfigurationBuilderHolder configuration)
   {
      for (StoreConfigurationBuilder<?, ?> store : configuration.getDefaultConfigurationBuilder().persistence().stores())
      {
         if (store instanceof JdbcStringBasedStoreConfigurationBuilder jdbcStore)
         {
            return jdbcStore;
         }
      }
      return null;
   }

   /**
    * Removes the lock properties of the given nodes, in one change, as the lock remover removes those of an expired
    * lock. When another node removed some of them meanwhile, the nodes are released one by one, as they are at their
    * turn: those whose lock properties are gone are skipped.
    *
    * @return the number of nodes whose lock properties were removed
    */
   int releaseLocks(List<String> nodeIdentifiers) throws RepositoryException
   {
      TransactionChangesLog changes = new TransactionChangesLog();
      int released = 0;
      for (String nodeIdentifier : nodeIdentifiers)
      {
         PlainChangesLog unlock = getUnlockChanges(nodeIdentifier);
         if (unlock != null)
         {
            changes.addLog(unlock);
            released++;
         }
      }
      if (released == 0)
      {
         return 0;
      }
      try
      {
         dataManager.save(changes);
         return released;
      }
      catch (JCRInvalidItemStateException e)
      {
         LOG.debug("Lock properties were removed meanwhile, the nodes are released one by one", e);
      }
      released = 0;
      for (String nodeIdentifier : nodeIdentifiers)
      {
         PlainChangesLog unlock = getUnlockChanges(nodeIdentifier);
         if (unlock != null)
         {
            try
            {
               dataManager.save(new TransactionChangesLog(unlock));
               released++;
            }
            catch (JCRInvalidItemStateException e)
            {
               LOG.debug("The lock properties of the node {} were removed meanwhile", nodeIdentifier, e);
            }
         }
      }
      return released;
   }

   /**
    * The change that removes the lock properties of a node, as the system, or null when the node holds none.
    */
   private PlainChangesLog getUnlockChanges(String nodeIdentifier) throws RepositoryException
   {
      NodeData node = (NodeData)dataManager.getItemData(nodeIdentifier);
      if (node == null)
      {
         return null;
      }
      PlainChangesLog unlock =
         new PlainChangesLogImpl(new ArrayList<ItemState>(), IdentityConstants.SYSTEM, ExtendedEvent.UNLOCK);
      for (InternalQName name : new InternalQName[]{Constants.JCR_LOCKOWNER, Constants.JCR_LOCKISDEEP})
      {
         PropertyData property =
            (PropertyData)dataManager.getItemData(node, new QPathEntry(name, 1), ItemType.PROPERTY);
         if (property != null)
         {
            unlock.add(ItemState.createDeletedState(copyItemData(property)));
         }
      }
      return unlock.getSize() > 0 ? unlock : null;
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
      return (LockData)PrivilegedISPNCacheHelper.putIfAbsent(lockStore(), lockData.getNodeIdentifier(), lockData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doRemove(LockData lockData)
   {
      lockStore().remove(lockData.getNodeIdentifier());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isAloneInCluster()
   {
      AdvancedCache<Serializable, Object> lockCache = lockStore();
      return lockCache.getRpcManager() == null || lockCache.getCacheManager().getMembers().size() == 1;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doClean()
   {
      if (lockStore().getStatus() == ComponentStatus.RUNNING)
      {
         for (LockData lockData : getLockList())
         {
            doRemove(lockData);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public LockTableHandler getLockTableHandler()
   {
      return new ISPNLockTableHandler(config, dataSource, lockTable);
   }

   @SuppressWarnings("rawtypes")
   @Listener
   public class CacheSizeEventListener
   {

      @CacheEntryCreated
      @CacheEntryRemoved
      public void CacheEntryRemoved(CacheEntryEvent evt)
      {
         if (!evt.isPre() && evt.getKey() instanceof String)
         {
            sizeCount.addAndGet(evt.getCache().size());
         }
      }
   }
}
