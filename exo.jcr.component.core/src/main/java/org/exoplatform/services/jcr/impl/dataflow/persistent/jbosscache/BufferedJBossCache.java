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
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.JBossCacheWorkspaceStorageCache.FakeValueSet;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.exoplatform.services.jcr.jbosscache.PrivilegedJBossCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.InvocationContext;
import org.jboss.cache.Node;
import org.jboss.cache.NodeNotExistsException;
import org.jboss.cache.Region;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.Configuration.CacheMode;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import org.jboss.cache.factories.ComponentRegistry;
import org.jboss.cache.interceptors.base.CommandInterceptor;
import org.jboss.cache.lock.LockManager;
import org.jboss.cache.transaction.GlobalTransaction;
import org.jgroups.Address;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Decorator over the JBossCache that stores changes in buffer, then sorts and applies to JBossCache.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: BufferedJBossCache.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
@SuppressWarnings("unchecked")
public class BufferedJBossCache implements Cache<Serializable, Object>
{
   /**
    * Parent cache.
    */
   private final CacheSPI<Serializable, Object> parentCache;

   private final ThreadLocal<CompressedChangesBuffer> changesList = new ThreadLocal<CompressedChangesBuffer>();

   private ThreadLocal<Boolean> local = new ThreadLocal<Boolean>();

   private final boolean useExpiration;

   private final long expirationTimeOut;

   private final TransactionManager tm;

   private volatile LockManager lm;

   protected static final Log LOG =
      ExoLogger.getLogger("org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache");

   public BufferedJBossCache(Cache<Serializable, Object> parentCache, boolean useExpiration, long expirationTimeOut)
   {
      super();
      this.parentCache = (CacheSPI<Serializable, Object>)parentCache;
      this.tm =
         this.parentCache.getTransactionManager() == null ? parentCache.getConfiguration().getRuntimeConfig()
            .getTransactionManager() : this.parentCache.getTransactionManager();
      this.useExpiration = useExpiration;
      this.expirationTimeOut = expirationTimeOut;
   }

   public LockManager getLockManager()
   {
      // We lazy get the LockManager to make sure that we get the right instance
      if (lm == null)
      {
         synchronized (this)
         {
            if (lm == null)
            {
               this.lm = parentCache.getComponentRegistry().getComponent(LockManager.class);
            }
         }
      }
      return lm;
   }

   /**
    * Start buffering process.
    */
   public void beginTransaction()
   {
      // We enabled the invalidation only if we have more than 1 cluster node and we are not in a global transaction
      changesList.set(new CompressedChangesBuffer(parentCache.getConfiguration().getCacheMode() != CacheMode.LOCAL
         && parentCache.getMembers().size() > 1 && parentCache.getTransactionTable().getCurrentTransaction(false) == null));
      local.set(false);
   }

   /**
    * 
    * @return status of the cache transaction
    */
   public boolean isTransactionActive()
   {
      return changesList.get() != null;
   }

   /**
    * Sort changes and commit data to the cache.
    */
   public void commitTransaction()
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      try
      {
         final List<ChangesContainer> containers = changesContainer.getSortedList();
         commitChanges(containers, changesContainer.isInvalidationEnabled());
      }
      finally
      {
         changesList.set(null);
         changesContainer = null;
      }
   }   

   /**
    * @param containers
    */
   private void commitChanges(List<ChangesContainer> containers, boolean invalidationEnabled)
   {
      if (invalidationEnabled)
      {
         // The invalidation is enabled
         Transaction tx = null;
         GlobalTransaction gt = null;
         // First we invalidate the cache entries outside the current transaction
         // to limit the risk of getting deadlocks
         try
         {
            for (Iterator<ChangesContainer> it = containers.iterator(); it.hasNext();)
            {
               ChangesContainer cacheChange = it.next();
               if (cacheChange.isInvalidation())
               {
                  // The current cache change is a cache entry to invalidate
                  if (gt == null)
                  {
                     // We get the current global transaction
                     gt = parentCache.getCurrentTransaction();
                     if (gt == null)
                     {
                        // There is no current transaction so no need to continue
                        break;
                     }
                  }
                  // We get the current owner
                  Object owner = getLockManager().getWriteOwner(cacheChange.getFqn());
                  if (owner == null || !owner.equals(gt))
                  {
                     // The node is not locked or is locked by another transaction so
                     if (tx == null)
                     {
                        try
                        {
                           tx = tm.suspend();
                        }
                        catch (Exception e)//NOSONAR
                        {
                           LOG.warn("Could not suspend the tx", e);
                        }
                     }
                     if (tx != null)
                     {
                        // We apply the invalidation outside the transaction
                        cacheChange.apply();
                        // We remove it to avoid calling it twice
                        it.remove();
                     }
                  }
               }
            }
         }
         finally
         {
            if (tx != null)
            {
               try
               {
                  tm.resume(tx);
               }
               catch (Exception e)//NOSONAR
               {
                  LOG.error("Could not resume the tx", e);
               }
            }
         }
      }
      for (ChangesContainer cacheChange : containers)
      {
         boolean isTxCreated = false;
         try
         {
            if (cacheChange.isTxRequired() && tm != null && tm.getStatus() == Status.STATUS_NO_TRANSACTION)
            {
               // No tx exists so we create a new tx
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("No Tx is active we then create a new tx");
               }
               tm.begin();
               isTxCreated = true;
            }
         }
         catch (Exception e)//NOSONAR
         {
            LOG.warn("Could not create a new tx", e);
         }
         try
         {
            cacheChange.apply();
         }
         catch (RuntimeException e)//NOSONAR
         {
            if (isTxCreated)
            {
               try
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An error occurs the tx will be rollbacked");
                  }
                  // Set the cache local mode to avoid unnecessary replication on roll back
                  parentCache.getInvocationContext().getTransactionContext().getOption().setCacheModeLocal(cacheChange.localMode);
                  tm.rollback();
               }
               catch (Exception e1)//NOSONAR
               {
                  LOG.warn("Could not rollback the tx", e1);
               }
            }
            throw e;
         }
         if (isTxCreated)
         {
            try
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("The tx will be committed");
               }
               // Set the cache local mode to avoid unnecessary replication on commit
               parentCache.getInvocationContext().getTransactionContext().getOption().setCacheModeLocal(cacheChange.localMode);
               tm.commit();
            }
            catch (Exception e)//NOSONAR
            {
               LOG.warn("Could not commit the tx", e);
            }
         }
      }
   }

   /**
    * Tries to get buffer and if it is null throws an exception otherwise returns buffer. 
    * 
    * @return
    */
   private CompressedChangesBuffer getChangesBufferSafe()
   {
      CompressedChangesBuffer changesContainer = changesList.get();
      if (changesContainer == null)
      {
         throw new IllegalStateException("changesContainer should not be empty");
      }
      return changesContainer;
   }

   /**
    * Forget about changes
    */
   public void rollbackTransaction()
   {
      changesList.set(null);
   }

   /**
    * Creates all ChangesBuffers with given parameter
    * 
    * @param local
    */
   public void setLocal(boolean local)
   {
      //start local transaction
      if (local && changesList.get() == null)
      {
         beginTransaction();
      }
      this.local.set(local);
   }

   /**
    * Returns current state.
    */
   public boolean isLocal()
   {
      return this.local.get();
   }

   public int getNumberOfNodes()
   {
      return parentCache.getNumberOfNodes();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#addCacheListener(java.lang.Object)
    */
   public void addCacheListener(Object listener)
   {
      parentCache.addCacheListener(listener);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#addInterceptor(org.jboss.cache.interceptors.base.CommandInterceptor, java.lang.Class)
    */
   public void addInterceptor(CommandInterceptor i, Class<? extends CommandInterceptor> afterInterceptor)
   {
      parentCache.addInterceptor(i, afterInterceptor);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#addInterceptor(org.jboss.cache.interceptors.base.CommandInterceptor, int)
    */
   public void addInterceptor(CommandInterceptor i, int position)
   {
      parentCache.addInterceptor(i, position);

   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#clearData(org.jboss.cache.Fqn)
    */
   public void clearData(Fqn fqn)
   {
      parentCache.clearData(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#clearData(java.lang.String)
    */
   public void clearData(String fqn)
   {
      parentCache.clearData(fqn);

   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#create()
    */
   public void create() throws CacheException
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            parentCache.create();
            return null;
         }
      };
      SecurityHelper.doPrivilegedAction(action);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#destroy()
    */
   public void destroy()
   {
      parentCache.destroy();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#endBatch(boolean)
    */
   public void endBatch(boolean successful)
   {
      parentCache.endBatch(successful);

   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#evict(org.jboss.cache.Fqn, boolean)
    */
   public void evict(Fqn fqn, boolean recursive)
   {
      parentCache.evict(fqn, recursive);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#evict(org.jboss.cache.Fqn)
    */
   public void evict(Fqn fqn)
   {
      parentCache.evict(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#get(org.jboss.cache.Fqn, java.lang.Object)
    */
   public Object get(Fqn fqn, Serializable key)
   {
      return parentCache.get(fqn, key);
   }

   /**
    * Look for Object by Fqn and key in internal buffer. If Buffer do not contain object, than return from JBoss-cache.
    * 
    * @param fqn - Fqn 
    * @param key
    * @return
    */
   public Object getFromBuffer(Fqn fqn, Serializable key)
   {
      //look at buffer for first
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();

      Object objectFromBuffer = getObjectFromChangesContainer(changesContainer, fqn, key);

      if (objectFromBuffer != null)
      {
         return objectFromBuffer;
      }
      else
      {
         return parentCache.get(fqn, key);
      }
   }

   Map<Fqn, Map<Serializable, Object>> getLastChanges()
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      return changesContainer.getLastChanges();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#get(java.lang.String, java.lang.Object)
    */
   public Object get(String fqn, Serializable key)
   {
      return parentCache.get(fqn, key);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getCacheListeners()
    */
   public Set<Object> getCacheListeners()
   {
      return parentCache.getCacheListeners();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getCacheStatus()
    */
   public CacheStatus getCacheStatus()
   {
      return parentCache.getCacheStatus();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getChildrenNames(org.jboss.cache.Fqn)
    */
   public Set<Object> getChildrenNames(Fqn fqn)
   {
      return parentCache.getChildrenNames(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getChildrenNames(java.lang.String)
    */
   public Set<String> getChildrenNames(String fqn)
   {
      return parentCache.getChildrenNames(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getConfiguration()
    */
   public Configuration getConfiguration()
   {
      return parentCache.getConfiguration();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getData(org.jboss.cache.Fqn)
    */
   public Map<Serializable, Object> getData(Fqn fqn)
   {
      return parentCache.getData(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getInvocationContext()
    */
   public InvocationContext getInvocationContext()
   {
      return parentCache.getInvocationContext();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getKeys(org.jboss.cache.Fqn)
    */
   public Set<Serializable> getKeys(Fqn fqn)
   {
      return parentCache.getKeys(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getKeys(java.lang.String)
    */
   public Set<Serializable> getKeys(String fqn)
   {
      return parentCache.getKeys(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getLocalAddress()
    */
   public Address getLocalAddress()
   {
      return parentCache.getLocalAddress();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getMembers()
    */
   public List<Address> getMembers()
   {
      return parentCache.getMembers();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getNode(org.jboss.cache.Fqn)
    */
   public Node<Serializable, Object> getNode(Fqn fqn)
   {
      return parentCache.getNode(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getNode(java.lang.String)
    */
   public Node<Serializable, Object> getNode(String fqn)
   {
      return parentCache.getNode(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getRegion(org.jboss.cache.Fqn, boolean)
    */
   public Region getRegion(Fqn fqn, boolean createIfAbsent)
   {
      return parentCache.getRegion(fqn, createIfAbsent);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getRoot()
    */
   public Node<Serializable, Object> getRoot()
   {
      return parentCache.getRoot();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#getVersion()
    */
   public String getVersion()
   {
      return parentCache.getVersion();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#isLeaf(org.jboss.cache.Fqn)
    */
   public boolean isLeaf(Fqn fqn)
   {
      return parentCache.isLeaf(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#isLeaf(java.lang.String)
    */
   public boolean isLeaf(String fqn)
   {
      return parentCache.isLeaf(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#move(org.jboss.cache.Fqn, org.jboss.cache.Fqn)
    */
   public void move(Fqn nodeToMove, Fqn newParent) throws NodeNotExistsException
   {
      parentCache.move(nodeToMove, newParent);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#move(java.lang.String, java.lang.String)
    */
   public void move(String nodeToMove, String newParent) throws NodeNotExistsException
   {
      parentCache.move(nodeToMove, newParent);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#put(org.jboss.cache.Fqn, java.util.Map)
    */
   public void put(Fqn fqn, Map<? extends Serializable, ? extends Object> data)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutObjectContainer(fqn, data, parentCache, changesContainer.getHistoryIndex(), local
         .get(), useExpiration, expirationTimeOut));
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(Fqn fqn, Serializable key, Object value)
   {
      putOnly(fqn, key, value);

      return parentCache.get(fqn, key);
   }

   public void putOnly(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutKeyValueContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get(), useExpiration, expirationTimeOut));
   }

   /**
    * in case putIfAbsent is set to <code>true</code> this method will 
    * call cache.putIfAbsent(Fqn fqn, Serializable key, Object value)
    * otherwise it will call cache.put(Fqn fqn, Serializable key, Object value)
    */
   protected Object put(Fqn fqn, Serializable key, Object value, boolean putIfAbsent, boolean putOnly)
   {
      if (putIfAbsent)
      {
         putIfAbsent(fqn, key, value);
         return null;
      }
      else if (putOnly)
      {
         putOnly(fqn, key, value);
      }
      return put(fqn, key, value);
   }

   /**
    * This method will create and add a ChangesContainer that will put the value only if no value has been added
    */
   protected Object putIfAbsent(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutIfAbsentKeyValueContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get(), useExpiration, expirationTimeOut));
      return null;
   }

   /**
    * Removes an item from the cache if and only if the current value for the provided
    * fqn and key is equals to the provided item.
    */
   protected void remove(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveIfExistKeyContainer(fqn, key, value, parentCache, changesContainer
         .getHistoryIndex(), local.get(), useExpiration, expirationTimeOut));
   }

   public Object putInBuffer(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();

      // take Object from buffer for first 
      Object prevObject = getObjectFromChangesContainer(changesContainer, fqn, key);

      changesContainer.add(new PutKeyValueContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get(), useExpiration, expirationTimeOut));

      if (prevObject != null)
      {
         return prevObject;
      }
      else
      {
         return parentCache.get(fqn, key);
      }
   }

   private Object getObjectFromChangesContainer(CompressedChangesBuffer changesContainer, Fqn fqn, Serializable key)
   {
      return changesContainer.get(fqn, key);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#put(java.lang.String, java.util.Map)
    */
   public void put(String fqn, Map<? extends Serializable, ? extends Object> data)
   {
      throw new UnsupportedOperationException(
         "Unexpected method call use put(Fqn fqn, Map<? extends Serializable, ? extends Object> data)");
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#put(java.lang.String, java.lang.Object, java.lang.Object)
    */
   public Object put(String fqn, Serializable key, Object value)
   {
      throw new UnsupportedOperationException("Unexpected method call use put(Fqn fqn, Serializable key, Object value)");
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#putForExternalRead(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public void putForExternalRead(Fqn fqn, Serializable key, Object value)
   {
      throw new UnsupportedOperationException("Unexpected method call ");
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#remove(org.jboss.cache.Fqn, java.lang.Object)
    */
   public Object remove(Fqn fqn, Serializable key)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveKeyContainer(fqn, key, parentCache, changesContainer.getHistoryIndex(), local
         .get(), useExpiration, expirationTimeOut));
      return parentCache.get(fqn, key);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#remove(java.lang.String, java.lang.Object)
    */
   public Object remove(String fqn, Serializable key)
   {
      throw new UnsupportedOperationException("Unexpected method call. Use remove(Fqn fqn, Serializable key)");
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeCacheListener(java.lang.Object)
    */
   public void removeCacheListener(Object listener)
   {
      parentCache.removeCacheListener(listener);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeInterceptor(java.lang.Class)
    */
   public void removeInterceptor(Class<? extends CommandInterceptor> interceptorType)
   {
      parentCache.removeInterceptor(interceptorType);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeInterceptor(int)
    */
   public void removeInterceptor(int position)
   {
      parentCache.removeInterceptor(position);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeNode(org.jboss.cache.Fqn)
    */
   public boolean removeNode(Fqn fqn)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveNodeContainer(fqn, parentCache, changesContainer.getHistoryIndex(), local.get(),
         useExpiration, expirationTimeOut));
      return true;
   }

   /**
    * Invalidates the node
    */
   public void invalidateNode(Fqn fqn)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveNodeContainer(fqn, parentCache, changesContainer.getHistoryIndex(), local.get(),
         useExpiration, expirationTimeOut, true));
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeNode(java.lang.String)
    */
   public boolean removeNode(String fqn)
   {
      throw new UnsupportedOperationException("Unexpected method call. Use remove removeNode(Fqn fqn)");
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#removeRegion(org.jboss.cache.Fqn)
    */
   public boolean removeRegion(Fqn fqn)
   {
      return parentCache.removeRegion(fqn);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#setInvocationContext(org.jboss.cache.InvocationContext)
    */
   public void setInvocationContext(InvocationContext ctx)
   {
      parentCache.setInvocationContext(ctx);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#start()
    */
   public void start() throws CacheException
   {
      PrivilegedJBossCacheHelper.start(parentCache);
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#startBatch()
    */
   public void startBatch()
   {
      parentCache.startBatch();
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#stop()
    */
   public void stop()
   {
      try
      {
         ExoJBossCacheFactory.releaseUniqueInstance(CacheType.JCR_CACHE, parentCache);
      }
      catch (RepositoryConfigurationException e)
      {
         LOG.error("Can not release cache instance", e);
      }
   }

   /**
    * Returns the component registry associated with this cache instance.
    *
    * @see org.jboss.cache.factories.ComponentRegistry
    */
   public ComponentRegistry getComponentRegistry()
   {
      return parentCache.getComponentRegistry();
   }

   public TransactionManager getTransactionManager()
   {
      return parentCache.getTransactionManager();
   }

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back.
    * If null found, then new Set created (ordinary cache does).
    * 
    * @param fqn
    * @param key
    * @param value
    */
   public void addToList(Fqn fqn, String key, Object value, boolean forceModify)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new AddToListContainer(fqn, key, value, parentCache, forceModify, changesContainer
         .getHistoryIndex(), local.get(), useExpiration, expirationTimeOut));
   }

   /**
    * It tries to get Set by given key. If it is Set then adds new item UUID and puts new set back.
    * If null found, then new Set created (ordinary cache does). It doesn't actually puts an item,
    * it's UUID is written only.
    * 
    * @param fqn
    * @param patternKey
    * @param listKey
    * @param value
    */
   public void addToPatternList(Fqn fqn, String patternKey, String listKey, ItemData value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new AddToPatternListContainer(fqn, patternKey, listKey, value, parentCache, changesContainer
         .getHistoryIndex(), local.get(), useExpiration, expirationTimeOut));
   }

   /**
    * It tries to get set by given key. If it is set then removes value and puts new modified set back.
    * 
    * @param fqn
    * @param key
    * @param value
    */
   public void removeFromList(Fqn fqn, String key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveFromListContainer(fqn, key, value, parentCache,
         changesContainer.getHistoryIndex(), local.get(), useExpiration, expirationTimeOut));
   }

   public void removeFromPatternList(Fqn fqn, String patternKey, String listKey, ItemData value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveFromPatternListContainer(fqn, patternKey, listKey, value, parentCache,
         changesContainer.getHistoryIndex(), local.get(), useExpiration, expirationTimeOut));
   }

   public static enum ChangesType {
      REMOVE, REMOVE_KEY, PUT, PUT_KEY, PUT_TO_LIST;
   }

   /**
    * Container for changes
    */
   public static abstract class ChangesContainer implements Comparable<ChangesContainer>
   {
      protected final Fqn fqn;

      protected final ChangesType changesType;

      protected final Cache<Serializable, Object> cache;

      protected final int historicalIndex;

      protected final boolean localMode;

      protected final boolean useExpiration;

      protected final long timeOut;

      protected boolean invalidation;

      public ChangesContainer(Fqn fqn, ChangesType changesType, Cache<Serializable, Object> cache, int historicalIndex,
         boolean localMode, boolean useExpiration, long timeOut)
      {
         this(fqn, changesType, cache, historicalIndex, localMode, useExpiration, timeOut, false);
      }

      public ChangesContainer(Fqn fqn, ChangesType changesType, Cache<Serializable, Object> cache, int historicalIndex,
         boolean localMode, boolean useExpiration, long timeOut, boolean invalidation)
      {
         super();
         this.fqn = fqn;
         this.changesType = changesType;
         this.cache = cache;
         this.historicalIndex = historicalIndex;
         this.localMode = localMode;
         this.useExpiration = useExpiration;
         this.timeOut = timeOut;
         this.invalidation = invalidation && !localMode;
      }

      /**
       * @return the fqn
       */
      public Fqn getFqn()
      {
         return fqn;
      }

      /**
       * @return the index of change in original sequence
       */
      public int getHistoricalIndex()
      {
         return historicalIndex;
      }

      /**
       * @return the changesType
       */
      public ChangesType getChangesType()
      {
         return changesType;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return fqn + " type=" + changesType + " historyIndex=" + historicalIndex;
      }

      public int compareTo(ChangesContainer o)
      {
         int result = fqn.compareTo(o.getFqn());
         return result == 0 ? historicalIndex - o.getHistoricalIndex() : result;
      }

      protected void setCacheLocalMode()
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
      }

      public final void putExpiration(Fqn efqn)
      {
         setCacheLocalMode();
         cache.put(efqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + timeOut));
      }

      public abstract void apply();

      public boolean isTxRequired()
      {
         return false;
      }

      public boolean isInvalidation()
      {
         return invalidation;
      }

      void setInvalidation(boolean invalidationEnabled)
      {
         invalidation &= invalidationEnabled;
      }

      void applyToBuffer(CompressedChangesBuffer buffer)
      {
         // By default we don't do anything
      }
   }

   /**
    * Put object container;
    */
   public static class PutObjectContainer extends ChangesContainer
   {
      private final Map<? extends Serializable, ? extends Object> data;

      public PutObjectContainer(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.PUT, cache, historicalIndex, local, useExpiration, timeOut);

         this.data = data;
      }

      @Override
      public void apply()
      {
         if (useExpiration)
         {
            putExpiration(fqn);
         }
         
         setCacheLocalMode();
         cache.put(fqn, data);
      }
   }

   /**
    * PutIfAbsent  container.
    */
   public static class PutIfAbsentKeyValueContainer extends ChangesContainer
   {
      private final Serializable key;

      private final Object value;

      public PutIfAbsentKeyValueContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local, useExpiration, timeOut);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         if (!localMode && value instanceof FakeValueSet)
         {
            // Ensure that a FakeValueSet won't be replicated
            return;
         }
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         Object oldValue;
         if ((oldValue = cache.get(fqn, key)) != null && !(oldValue instanceof FakeValueSet))
         {
            // a value already exists and it is not a FakeValueSet
            return;
         }
         if (useExpiration)
         {
            putExpiration(fqn);
         }

         setCacheLocalMode();
         cache.put(fqn, key, value);
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * Put  container.
    */
   public static class PutKeyValueContainer extends ChangesContainer
   {
      private final Serializable key;

      private final Object value;

      public PutKeyValueContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local, useExpiration, timeOut);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         if (useExpiration)
         {
            putExpiration(fqn);
         }

         setCacheLocalMode();
         cache.put(fqn, key, value);
      }

      @Override
      void applyToBuffer(CompressedChangesBuffer buffer)
      {
         buffer.put(fqn, key, value);
      }
   }

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back.
    * If null found, then new Set created (ordinary cache does).
    */
   public static class AddToListContainer extends ChangesContainer
   {
      private final Serializable key;

      private final Object value;
      
      private final boolean forceModify;

      public AddToListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         boolean forceModify, int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local, useExpiration, timeOut, true);
         this.key = key;
         this.value = value;
         this.forceModify = forceModify;
      }

      @Override
      public void apply()
      {
         if (invalidation)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we clear the list in order to enforce other cluster nodes to reload it from the db
            cache.removeNode(fqn);
            return;
         }
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         // object found by FQN and key;
         Object existingObject = cache.get(getFqn(), key);
         Set<Object> newSet = new HashSet<Object>();
         // if set found or null, perform add
         if (existingObject instanceof Set || (existingObject == null && forceModify))
         {
            // set found
            if (existingObject instanceof FakeValueSet)
            {
               // remove the FakeValueSet instance as it is not up to date anymore
               setCacheLocalMode();
               cache.put(fqn, key, null);
               return;
            }
            else if (existingObject instanceof Set)
            {
               newSet.addAll((Set<Object>)existingObject);
            }
            newSet.add(value);

            if (useExpiration)
            {
               putExpiration(fqn);
            }

            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
         else if (existingObject != null)
         {
            LOG.error("Unexpected object found by FQN:" + getFqn() + " and key:" + key + ". Expected Set, but found:"
               + existingObject.getClass().getName());
         }
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get all child pattern nodes. Then iterate patterns and adds item ID to acceptable pattern nodes list.
    */
   public static class AddToPatternListContainer extends ChangesContainer
   {
      private final Serializable patternKey;

      private final Serializable listKey;

      private final ItemData value;

      public AddToPatternListContainer(Fqn fqn, Serializable patternKey, Serializable listKey, ItemData value,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local, useExpiration, timeOut, true);
         this.patternKey = patternKey;
         this.listKey = listKey;
         this.value = value;
      }

      @Override
      public void apply()
      {
         if (invalidation)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we remove all the patterns in order to enforce other cluster nodes 
            // to reload them from the db
            cache.removeNode(fqn);
            return;
         }
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         Iterator<Object> patternNames = cache.getChildrenNames(fqn).iterator();
         while (patternNames.hasNext())
         {
            Object name = patternNames.next();
            Fqn<Object> patternFqn = Fqn.fromRelativeElements(fqn, name);
            cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
            Object patternObject = cache.get(patternFqn, patternKey);
            if (!(patternObject instanceof QPathEntryFilter))
            {
               LOG.error("Unexpected object found by FQN:" + patternFqn + " and key:" + patternKey
                  + ". Expected QPathEntryFilter, but found:" + patternObject.getClass().getName());
               continue;
            }
            QPathEntryFilter nameFilter = (QPathEntryFilter)patternObject;
            if (nameFilter.accept(value))
            {
               cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
               Object setObject = cache.get(patternFqn, listKey);
               if (!(setObject instanceof Set))
               {
                  LOG.error("Unexpected object found by FQN:" + patternFqn + " and key:" + listKey
                     + ". Expected Set, but found:" + setObject.getClass().getName());
                  continue;
               }
               Set<String> newSet = new HashSet<String>((Set<String>)setObject);
               newSet.add(value.getIdentifier());

               if (useExpiration)
               {
                  putExpiration(fqn);
               }

               setCacheLocalMode();
               cache.put(patternFqn, listKey, newSet);
            }
         }
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get set by given key. If it is set then removes value and puts new modified set back.
    */
   public static class RemoveFromListContainer extends ChangesContainer
   {
      private final Serializable key;

      private final Object value;

      public RemoveFromListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local, useExpiration, timeOut, true);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         if (invalidation)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we clear the list in order to enforce other cluster nodes to reload it from the db
            cache.removeNode(fqn);
            return;
         }
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         Object existingObject = cache.get(getFqn(), key);
         // if found value is really set! add to it.
         if (existingObject instanceof FakeValueSet)
         {
            // remove the FakeValueSet instance as it is not up to date anymore
            setCacheLocalMode();
            cache.put(fqn, key, null);
            return;
         }
         else if (existingObject instanceof Set)
         {
            Set<Object> newSet = new HashSet<Object>((Set<Object>)existingObject);
            newSet.remove(value);

            if (useExpiration)
            {
               putExpiration(fqn);
            }

            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get all child pattern nodes. Then iterate patterns and removes item IDs from acceptable pattern nodes list.
    */
   public static class RemoveFromPatternListContainer extends ChangesContainer
   {
      private final Serializable patternKey;

      private final Serializable listKey;

      private final ItemData value;

      public RemoveFromPatternListContainer(Fqn fqn, Serializable patternKey, Serializable listKey, ItemData value,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local, useExpiration, timeOut, true);
         this.patternKey = patternKey;
         this.listKey = listKey;
         this.value = value;
      }

      @Override
      public void apply()
      {
         if (invalidation)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we clear the list in order to enforce other cluster nodes to reload it from the db
            cache.removeNode(fqn);
            return;
         }
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         Iterator<Object> patternNames = cache.getChildrenNames(fqn).iterator();
         while (patternNames.hasNext())
         {
            Fqn<Object> patternFqn = Fqn.fromRelativeElements(fqn, patternNames.next());
            cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
            Object patternObject = cache.get(patternFqn, patternKey);
            if (!(patternObject instanceof QPathEntryFilter))
            {
               LOG.error("Unexpected object found by FQN:" + patternFqn + " and key:" + patternKey
                  + ". Expected QPathEntryFilter, but found:" + patternObject.getClass().getName());
               continue;
            }
            QPathEntryFilter nameFilter = (QPathEntryFilter)patternObject;
            if (nameFilter.accept(value))
            {
               cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
               Object setObject = cache.get(patternFqn, listKey);
               if (!(setObject instanceof Set))
               {
                  LOG.error("Unexpected object found by FQN:" + patternFqn + " and key:" + listKey
                     + ". Expected Set, but found:" + setObject.getClass().getName());
                  continue;
               }
               Set<String> newSet = new HashSet<String>((Set<String>)setObject);
               newSet.remove(value.getIdentifier());

               if (useExpiration)
               {
                  putExpiration(fqn);
               }

               setCacheLocalMode();
               cache.put(patternFqn, listKey, newSet);
            }
         }
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * Remove container.
    */
   public static class RemoveKeyContainer extends ChangesContainer
   {
      private final Serializable key;

      public RemoveKeyContainer(Fqn fqn, Serializable key, Cache<Serializable, Object> cache, int historicalIndex,
         boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local, useExpiration, timeOut);
         this.key = key;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.remove(fqn, key);
      }
   }

   /**
    * Remove container.
    */
   public static class RemoveIfExistKeyContainer extends ChangesContainer
   {
      private final Serializable key;

      private final Object value;

      public RemoveIfExistKeyContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local, boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local, useExpiration, timeOut);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         Object existingObject = cache.get(getFqn(), key);
         if (existingObject != null && existingObject.equals(value))
         {
            setCacheLocalMode();
            cache.remove(fqn, key);
         }
      }
   }

   /**
    * Remove container.
    */
   public static class RemoveNodeContainer extends ChangesContainer
   {

      public RemoveNodeContainer(Fqn fqn, Cache<Serializable, Object> cache, int historicalIndex, boolean local,
         boolean useExpiration, long timeOut)
      {
         super(fqn, ChangesType.REMOVE, cache, historicalIndex, local, useExpiration, timeOut);
      }

      public RemoveNodeContainer(Fqn fqn, Cache<Serializable, Object> cache, int historicalIndex, boolean local,
         boolean useExpiration, long timeOut, boolean invalidation)
      {
         super(fqn, ChangesType.REMOVE, cache, historicalIndex, local, useExpiration, timeOut, invalidation);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.removeNode(fqn);
      }

      void applyToBuffer(CompressedChangesBuffer buffer)
      {
         buffer.put(fqn, null, null);
      }
   }
}
