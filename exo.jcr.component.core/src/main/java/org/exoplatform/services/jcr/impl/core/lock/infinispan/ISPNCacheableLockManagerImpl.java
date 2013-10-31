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
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandler;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.CacheableSessionLockManager;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.LockData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.transaction.TransactionService;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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

   public static final String INFINISPAN_JDBC_CL_ID_COLUMN_NAME = "infinispan-cl-cache.jdbc.id.column";

   public static final String INFINISPAN_JDBC_TABLE_NAME = "infinispan-cl-cache.jdbc.table.name";

   public static final String INFINISPAN_JDBC_CL_AUTO = "auto";

   /**
    * Logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.InfinispanLockManagerImpl");//NOSONAR

   private AdvancedCache<Serializable, Object> cache;

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
         ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm, transactionManager);

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
            INFINISPAN_JDBC_CL_DATA_COLUMN, INFINISPAN_JDBC_CL_ID_COLUMN, INFINISPAN_JDBC_CL_TIMESTAMP_COLUMN);

         cache =
            factory.createCache("L" + config.getUniqueName().replace("_", ""), config.getLockManager())
               .getAdvancedCache();
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
            return cache.withFlags(Flag.SKIP_CACHE_LOAD).containsKey(nodeId);
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
            return cache.withFlags(Flag.SKIP_CACHE_LOAD).containsKey(nodeId);
         }
      };

      this.getLockDataById = new LockActionNonTxAware<LockData, String>()
      {
         public LockData execute(String nodeId) throws LockException
         {
            return (LockData)cache.withFlags(Flag.SKIP_CACHE_LOAD).get(nodeId);
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
    * {@inheritDoc}
    */
   public void start()
   {
      PrivilegedISPNCacheHelper.start(cache);
      super.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      super.stop();
      PrivilegedISPNCacheHelper.stop(cache);
      ISPNCacheFactory.releaseUniqueInstance(cache.getCacheManager());
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
      return cache.getRpcManager() == null || cache.getCacheManager().getMembers().size() == 1;
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

   /**
    * {@inheritDoc}
    */
   public LockTableHandler getLockTableHandler()
   {
      return new ISPNLockTableHandler(config, dataSource);
   }
}
