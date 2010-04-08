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
import org.jboss.cache.interceptors.base.CommandInterceptor;
import org.jgroups.Address;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   //   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.BufferedJbossCache");

   /**
    * Parent cache.
    */
   private final Cache<Serializable, Object> parentCache;

   private final ThreadLocal<CompressedChangesBuffer> changesList = new ThreadLocal<CompressedChangesBuffer>();

   private ThreadLocal<Boolean> local = new ThreadLocal<Boolean>();

   protected static final Log LOG =
      ExoLogger.getLogger("org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache");

   public BufferedJBossCache(Cache<Serializable, Object> parentCache)
   {
      super();
      this.parentCache = parentCache;
   }

   /**
    * Start buffering process.
    */
   public void beginTransaction()
   {

      changesList.set(new CompressedChangesBuffer());
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
         //log.info("Before=" + changesContainer.toString());
         //Collections.sort(changesContainer);
         List<ChangesContainer> containers = changesContainer.getSortedList();
         //log.info("After=" + changesContainer.toString());
         for (ChangesContainer cacheChange : containers)
         {
            cacheChange.apply();
         }
      }
      finally
      {
         changesList.set(null);
         changesContainer = null;
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
      if (!local && this.local.get())
      {

      }
      this.local.set(local);
   }

   public int getNumberOfNodes()
   {
      return ((CacheSPI<Serializable, Object>)parentCache).getNumberOfNodes();
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
      parentCache.create();
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
         .get()));
   }

   /* (non-Javadoc)
    * @see org.jboss.cache.Cache#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutKeyValueContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get()));

      return parentCache.get(fqn, key);
   }

   public Object putInBuffer(Fqn fqn, Serializable key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();

      // take Object from buffer for first 
      Object prevObject = getObjectFromChangesContainer(changesContainer, fqn, key);

      changesContainer.add(new PutKeyValueContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get()));

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
      List<ChangesContainer> changes = changesContainer.getSortedList();
      Object object = null;
      for (ChangesContainer change : changes)
      {
         if (change.getChangesType().equals(ChangesType.PUT_KEY) && change.getFqn().equals(fqn))
         {
            PutKeyValueContainer cont = ((PutKeyValueContainer)change);
            if (cont.key.equals(key))
            {
               object = ((PutKeyValueContainer)change).value;
            }
         }
      }

      return object;
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
         .get()));
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
      changesContainer.add(new RemoveNodeContainer(fqn, parentCache, changesContainer.getHistoryIndex(), local.get()));
      return true;
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
      parentCache.start();
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
      parentCache.stop();
   }

   public TransactionManager getTransactionManager()
   {
      return ((CacheSPI<Serializable, Object>)parentCache).getTransactionManager();
   }

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back.
    * If null found, then new Set created (ordinary cache does).
    * 
    * @param fqn
    * @param key
    * @param value
    */
   public void addToList(Fqn fqn, String key, Object value)
   {
      CompressedChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new AddToListContainer(fqn, key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get()));
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
         changesContainer.getHistoryIndex(), local.get()));
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

      public ChangesContainer(Fqn fqn, ChangesType changesType, Cache<Serializable, Object> cache, int historicalIndex,
         boolean localMode)
      {
         super();
         this.fqn = fqn;
         this.changesType = changesType;
         this.cache = cache;
         this.historicalIndex = historicalIndex;
         this.localMode = localMode;
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

      public abstract void apply();
   }

   /**
    * Put object container;
    */
   public static class PutObjectContainer extends ChangesContainer
   {
      private final Map<? extends Serializable, ? extends Object> data;

      public PutObjectContainer(Fqn fqn, Map<? extends Serializable, ? extends Object> data,
         Cache<Serializable, Object> cache, int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT, cache, historicalIndex, local);

         this.data = data;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, data);
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
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.put(fqn, key, value);
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

      public AddToListContainer(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache,
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.PUT_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         // object found by FQN and key;
         Object existingObject = cache.get(getFqn(), key);
         Set<Object> newSet = new HashSet<Object>();
         // if set found of null, perform add
         if (existingObject instanceof Set || existingObject == null)
         {
            // set found
            if (existingObject instanceof Set)
            {
               newSet.addAll((Set<Object>)existingObject);
            }
            newSet.add(value);
            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
         else
         {
            LOG.error("Unexpected object found by FQN:" + getFqn() + " and key:" + key + ". Expected Set, but found:"
               + existingObject.getClass().getName());
         }
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
         int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local);
         this.key = key;
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         cache.getInvocationContext().getOptionOverrides().setForceWriteLock(true);
         // object found by FQN and key;
         setCacheLocalMode();
         Object existingObject = cache.get(getFqn(), key);
         // if found value is really set! add to it.
         if (existingObject instanceof Set)
         {
            Set<Object> newSet = new HashSet<Object>((Set<Object>)existingObject);
            newSet.remove(value);
            setCacheLocalMode();
            cache.put(fqn, key, newSet);
         }
      }
   }

   /**
    * Remove container.
    */
   public static class RemoveKeyContainer extends ChangesContainer
   {
      private final Serializable key;

      public RemoveKeyContainer(Fqn fqn, Serializable key, Cache<Serializable, Object> cache, int historicalIndex,
         boolean local)
      {
         super(fqn, ChangesType.REMOVE_KEY, cache, historicalIndex, local);
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
   public static class RemoveNodeContainer extends ChangesContainer
   {

      public RemoveNodeContainer(Fqn fqn, Cache<Serializable, Object> cache, int historicalIndex, boolean local)
      {
         super(fqn, ChangesType.REMOVE, cache, historicalIndex, local);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode();
         cache.removeNode(fqn);
      }
   }
}
