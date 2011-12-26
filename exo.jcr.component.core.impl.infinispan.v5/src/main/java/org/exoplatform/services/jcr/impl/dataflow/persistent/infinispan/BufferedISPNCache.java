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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.infinispan.util.concurrent.locks.LockManager;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

/**
 * Decorator over the Infinispan Cache that stores changes in buffer, then sorts and applies it.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: BufferedISPNCache.java 3514 2010-11-22 16:14:36Z nzamosenchuk $
 * 
 */
@SuppressWarnings("unchecked")
public class BufferedISPNCache implements Cache<CacheKey, Object>
{
   /**
    * Parent cache.
    */
   private final AdvancedCache<CacheKey, Object> parentCache;

   private final ThreadLocal<CompressedISPNChangesBuffer> changesList = new ThreadLocal<CompressedISPNChangesBuffer>();

   private ThreadLocal<Boolean> local = new ThreadLocal<Boolean>();

   /**
    * Allow to perform local cache changes.
    */
   private final Boolean allowLocalChanges;

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.BufferedISPNCache");

   public static enum ChangesType {
      REMOVE, PUT;
   }

   /**
    * Container for changes
    */
   public static abstract class ChangesContainer implements Comparable<ChangesContainer>
   {
      protected final CacheKey key;

      protected final ChangesType changesType;

      protected final AdvancedCache<CacheKey, Object> cache;

      protected final int historicalIndex;

      protected final boolean localMode;

      private final Boolean allowLocalChanges;

      public ChangesContainer(CacheKey key, ChangesType changesType, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean localMode, Boolean allowLocalChanges)
      {
         this.key = key;
         this.changesType = changesType;
         this.cache = cache;
         this.historicalIndex = historicalIndex;
         this.localMode = localMode;
         this.allowLocalChanges = allowLocalChanges;
      }

      /**
       * @return the key
       */
      public CacheKey getKey()
      {
         return key;
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

      /**
       * {@inheritDoc}
       */
      @Override
      public String toString()
      {
         return key.toString() + " type=" + changesType + " historysIndex=" + historicalIndex;
      }

      /**
       * {@inheritDoc}
       */
      public int compareTo(ChangesContainer o)
      {
         // We use the lock manager to sort the key in order to be able
         // to use the lock stripping more details here https://issues.jboss.org/browse/ISPN-993
         LockManager lm = cache.getLockManager();
         int result = lm.getLockId(key) - lm.getLockId(o.getKey());
         return result == 0 ? historicalIndex - o.getHistoricalIndex() : result;
      }

      protected AdvancedCache<CacheKey, Object> setCacheLocalMode()
      {
         if (localMode)
         {
            if (allowLocalChanges == null)
            {
               CacheMode cacheMode = cache.getConfiguration().getCacheMode();
               if (cacheMode != CacheMode.DIST_ASYNC && cacheMode != CacheMode.DIST_SYNC)
               {
                  return cache.withFlags(Flag.CACHE_MODE_LOCAL);
               }
            }
            else if (allowLocalChanges)
            {
               return cache.withFlags(Flag.CACHE_MODE_LOCAL);
            }
         }
         return cache;
      }

      public abstract void apply();

      public boolean isTxRequired()
      {
         return false;
      }
   }

   /**
    * Put object container;
    */
   public static class PutObjectContainer extends ChangesContainer
   {
      private final Object value;

      public PutObjectContainer(CacheKey key, Object value, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.PUT, cache, historicalIndex, local, allowLocalChanges);

         this.value = value;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, value);
      }
   }
   

   /**
    * Put object if absent container
    */
   public static class PutObjectIfAbsentContainer extends ChangesContainer
   {
      private final Object value;

      public PutObjectIfAbsentContainer(CacheKey key, Object value, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.PUT, cache, historicalIndex, local, allowLocalChanges);

         this.value = value;
      }

      @Override
      public void apply()
      {
         setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).putIfAbsent(key, value);
      }
   }   

   /**
    * It tries to get Set by given key. If it is Set then adds new value and puts new set back. If
    * null found, then new Set created (ordinary cache does).
    */
   public static class AddToListContainer extends ChangesContainer
   {
      private final Object value;

      private final boolean forceModify;

      public AddToListContainer(CacheKey key, Object value, AdvancedCache<CacheKey, Object> cache,
         boolean forceModify, int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.PUT, cache, historicalIndex, local, allowLocalChanges);
         this.value = value;
         this.forceModify = forceModify;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         Object existingObject = cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);
         Set<Object> newSet = new HashSet<Object>();

         // if set found of null, perform add
         if (existingObject instanceof Set || (existingObject == null && forceModify))
         {
            // set found
            if (existingObject instanceof Set)
            {
               newSet.addAll((Set<Object>)existingObject);
            }
            newSet.add(value);

            setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, newSet);
         }
         else if (existingObject != null)
         {
            LOG.error("Unexpected object found by key " + key.toString() + ". Expected Set, but found:"
               + existingObject.getClass().getName());
         }
         else if (!localMode && cache.getConfiguration().getCacheMode() != CacheMode.LOCAL
            && cache.getCacheManager().getMembers().size() > 1)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we clear the list in order to enforce other cluster nodes to reload it from the db
            cache.withFlags(Flag.SKIP_REMOTE_LOOKUP).remove(key);
            return;
         }
      }
      
      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get Map{filter, Set{itemIds}} by given key. 
    * If Map exists then adds itemDatas ID to all acceptable pattern lists.
    */
   public static class AddToPatternListContainer extends ChangesContainer
   {
      private final ItemData itemData;

      public AddToPatternListContainer(CacheKey key, ItemData value, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.PUT, cache, historicalIndex, local, allowLocalChanges);
         this.itemData = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         Object existingObject = cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

         // if Map found , perform add
         if (existingObject instanceof Map)
         {
            Map<QPathEntryFilter, Set<String>> newMap =
               new HashMap<QPathEntryFilter, Set<String>>((Map<QPathEntryFilter, Set<String>>)existingObject);

            Iterator<QPathEntryFilter> iterator = newMap.keySet().iterator();
            while (iterator.hasNext())
            {
               QPathEntryFilter pattern = iterator.next();
               if (pattern.accept(itemData))
               {
                  Set<String> newSet = newMap.get(pattern);
                  newSet.add(itemData.getIdentifier());
                  newMap.put(pattern, newSet);
               }
            }

            setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, newMap);
         }
         else if (existingObject != null)
         {
            LOG.error("Unexpected object found by key " + key.toString() + ". Expected Map, but found:"
               + existingObject.getClass().getName());
         }
         else if (!localMode && cache.getConfiguration().getCacheMode() != CacheMode.LOCAL
            && cache.getCacheManager().getMembers().size() > 1)
         {
            // to prevent consistency issue since we don't have the list in the local cache, we are in cluster env
            // and we are in a non local mode, we remove all the patterns in order to enforce other cluster nodes 
            // to reload them from the db
            cache.withFlags(Flag.SKIP_REMOTE_LOOKUP).remove(key);
         }
      }

      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get set by given key. If it is set then removes value and puts new modified set
    * back.
    */
   public static class RemoveFromListContainer extends ChangesContainer
   {
      private final Object value;

      public RemoveFromListContainer(CacheKey key, Object value, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.REMOVE, cache, historicalIndex, local, allowLocalChanges);
         this.value = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         Object existingObject = cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

         // if found value is really set! add to it.
         if (existingObject instanceof Set)
         {
            Set<Object> newSet = new HashSet<Object>((Set<Object>)existingObject);
            newSet.remove(value);

            setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, newSet);
         }
      }
      
      @Override
      public boolean isTxRequired()
      {
         return true;
      }
   }

   /**
    * It tries to get Map{filter, Set{itemIds}} by given key. 
    * IfMap exists then removes item IDs from all acceptable pattern lists.
    */
   public static class RemoveFromPatternListContainer extends ChangesContainer
   {
      private final ItemData itemData;

      public RemoveFromPatternListContainer(CacheKey key, ItemData value, AdvancedCache<CacheKey, Object> cache,
         int historicalIndex, boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.REMOVE, cache, historicalIndex, local, allowLocalChanges);
         this.itemData = value;
      }

      @Override
      public void apply()
      {
         // force writeLock on next read
         Object existingObject = cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);

         // if found value is really set! add to it.
         if (existingObject instanceof Map)
         {
            Map<QPathEntryFilter, Set<String>> newMap =
               new HashMap<QPathEntryFilter, Set<String>>((HashMap<QPathEntryFilter, Set<String>>)existingObject);

            Iterator<QPathEntryFilter> patternInterator = newMap.keySet().iterator();
            while (patternInterator.hasNext())
            {
               QPathEntryFilter pattern = patternInterator.next();
               if (pattern.accept(itemData))
               {
                  Set<String> newSet = new HashSet<String>(newMap.get(pattern));
                  newSet.remove(itemData.getIdentifier());
                  newMap.put(pattern, newSet);
               }
            }

            setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, newMap);
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
   public static class RemoveObjectContainer extends ChangesContainer
   {
      public RemoveObjectContainer(CacheKey key, AdvancedCache<CacheKey, Object> cache, int historicalIndex,
         boolean local, Boolean allowLocalChanges)
      {
         super(key, ChangesType.REMOVE, cache, historicalIndex, local, allowLocalChanges);
      }

      @Override
      public void apply()
      {
         setCacheLocalMode().withFlags(Flag.SKIP_REMOTE_LOOKUP).remove(key);
      }
   }

   public BufferedISPNCache(Cache<CacheKey, Object> parentCache, Boolean allowLocalChanges)
   {
      this.parentCache = parentCache.getAdvancedCache();
      this.allowLocalChanges = allowLocalChanges;
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Void> clearAsync()
   {
      return parentCache.clearAsync();
   }

   /**
    * {@inheritDoc}
    */
   public void compact()
   {
      parentCache.compact();
   }

   /**
    * {@inheritDoc}
    */
   public void endBatch(boolean successful)
   {
      parentCache.endBatch(successful);
   }

   /**
    * {@inheritDoc}
    */
   public Set<java.util.Map.Entry<CacheKey, Object>> entrySet()
   {
      return parentCache.entrySet();
   }

   /**
    * {@inheritDoc}
    */
   public void evict(CacheKey key)
   {
      parentCache.evict(key);
   }

   /**
    * {@inheritDoc}
    */
   public AdvancedCache<CacheKey, Object> getAdvancedCache()
   {
      return parentCache.getAdvancedCache();
   }

   /**
    * {@inheritDoc}
    */
   public EmbeddedCacheManager getCacheManager()
   {
      return parentCache.getCacheManager();
   }

   /**
    * {@inheritDoc}
    */
   public Configuration getConfiguration()
   {
      return parentCache.getConfiguration();
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return parentCache.getName();
   }

   /**
    * {@inheritDoc}
    */
   public ComponentStatus getStatus()
   {
      return parentCache.getStatus();
   }

   /**
    * {@inheritDoc}
    */
   public String getVersion()
   {
      return parentCache.getVersion();
   }

   /**
    * {@inheritDoc}
    */
   public Set<CacheKey> keySet()
   {
      return parentCache.keySet();
   }

   /**
    * {@inheritDoc}
    */
   public Object put(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.put(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public Object put(CacheKey key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
      TimeUnit maxIdleTimeUnit)
   {
      return parentCache.put(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   /**
    * {@inheritDoc}
    */
   public void putAll(Map<? extends CacheKey, ? extends Object> map, long lifespan, TimeUnit unit)
   {
      parentCache.putAll(map, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public void putAll(Map<? extends CacheKey, ? extends Object> map, long lifespan, TimeUnit lifespanUnit,
      long maxIdleTime, TimeUnit maxIdleTimeUnit)
   {
      parentCache.putAll(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Void> putAllAsync(Map<? extends CacheKey, ? extends Object> data)
   {
      return parentCache.putAllAsync(data);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Void> putAllAsync(Map<? extends CacheKey, ? extends Object> data, long lifespan,
      TimeUnit unit)
   {
      return parentCache.putAllAsync(data, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Void> putAllAsync(Map<? extends CacheKey, ? extends Object> data, long lifespan,
      TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit)
   {
      return parentCache.putAllAsync(data, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putAsync(CacheKey key, Object value)
   {
      return parentCache.putAsync(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putAsync(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.putAsync(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putAsync(CacheKey key, Object value, long lifespan, TimeUnit lifespanUnit,
      long maxIdle, TimeUnit maxIdleUnit)
   {
      return parentCache.putAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
   }

   /**
    * {@inheritDoc}
    */
   public void putForExternalRead(CacheKey key, Object value)
   {
      parentCache.putForExternalRead(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public Object putIfAbsent(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.putIfAbsent(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public Object putIfAbsent(CacheKey key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
      TimeUnit maxIdleTimeUnit)
   {
      return parentCache.putIfAbsent(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putIfAbsentAsync(CacheKey key, Object value)
   {
      return parentCache.putIfAbsentAsync(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putIfAbsentAsync(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.putIfAbsentAsync(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> putIfAbsentAsync(CacheKey key, Object value, long lifespan,
      TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit)
   {
      return parentCache.putIfAbsentAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> removeAsync(Object key)
   {
      return parentCache.removeAsync(key);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Boolean> removeAsync(Object key, Object value)
   {
      return parentCache.removeAsync(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public Object replace(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.replace(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public boolean replace(CacheKey key, Object oldValue, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.replace(key, oldValue, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public Object replace(CacheKey key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
      TimeUnit maxIdleTimeUnit)
   {
      return parentCache.replace(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   /**
    * {@inheritDoc}
    */
   public boolean replace(CacheKey key, Object oldValue, Object value, long lifespan, TimeUnit lifespanUnit,
      long maxIdleTime, TimeUnit maxIdleTimeUnit)
   {
      return parentCache.replace(key, oldValue, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> replaceAsync(CacheKey key, Object value)
   {
      return parentCache.replaceAsync(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Boolean> replaceAsync(CacheKey key, Object oldValue, Object newValue)
   {
      return parentCache.replaceAsync(key, oldValue, newValue);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> replaceAsync(CacheKey key, Object value, long lifespan, TimeUnit unit)
   {
      return parentCache.replaceAsync(key, value, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Boolean> replaceAsync(CacheKey key, Object oldValue, Object newValue, long lifespan,
      TimeUnit unit)
   {
      return parentCache.replaceAsync(key, oldValue, newValue, lifespan, unit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> replaceAsync(CacheKey key, Object value, long lifespan, TimeUnit lifespanUnit,
      long maxIdle, TimeUnit maxIdleUnit)
   {
      return parentCache.replaceAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Boolean> replaceAsync(CacheKey key, Object oldValue, Object newValue, long lifespan,
      TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit)
   {
      return parentCache.replaceAsync(key, oldValue, newValue);
   }

   /**
    * {@inheritDoc}
    */
   public boolean startBatch()
   {
      return parentCache.startBatch();
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Object> values()
   {
      return parentCache.values();
   }

   /**
    * {@inheritDoc}
    */
   public Object putIfAbsent(CacheKey key, Object value)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutObjectIfAbsentContainer(key, value, parentCache, changesContainer.getHistoryIndex(), local
         .get(), allowLocalChanges));
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean remove(Object key, Object value)
   {
      return parentCache.remove(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public Object replace(CacheKey key, Object value)
   {
      return parentCache.replace(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public boolean replace(CacheKey key, Object oldValue, Object newValue)
   {
      return parentCache.replace(key, oldValue, newValue);
   }

   /**
    * {@inheritDoc}
    */
   public void clear()
   {
      parentCache.clear();
   }

   /**
    * {@inheritDoc}
    */
   public boolean containsKey(Object key)
   {
      return parentCache.containsKey(key);
   }

   /**
    * {@inheritDoc}
    */
   public boolean containsValue(Object value)
   {
      return parentCache.containsValue(value);
   }

   /**
    * {@inheritDoc}
    */
   public Object get(Object key)
   {
      return parentCache.get(key);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isEmpty()
   {
      return parentCache.isEmpty();
   }

   /**
    * Put object in cache.
    * 
    * @param key
    *          cache key
    * @param value
    *          cache value
    * @return
    *          always returns null  
    */
   public Object put(CacheKey key, Object value)
   {
      return put(key, value, false);
   }

   /**
    * Put object in cache.
    * @param key
    *          cache key
    * @param value
    *          cache value
    * @param withReturnValue
    *          indicates if a return value is expected
    * @return <code>null</code> if <code>withReturnValue</code> has been set to <code>false</code>
    * the previous value otherwise
    */
   public Object put(final CacheKey key, Object value, final boolean withReturnValue)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new PutObjectContainer(key, value, parentCache, changesContainer.getHistoryIndex(), local
         .get(), allowLocalChanges));

      return withReturnValue ? parentCache.get(key) : null;
   }

   /**
    * {@inheritDoc}
    */
   public void putAll(Map<? extends CacheKey, ? extends Object> m)
   {
      parentCache.putAll(m);
   }

   /**
    * {@inheritDoc}
    */
   public Object remove(Object key)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveObjectContainer((CacheKey)key, parentCache, changesContainer.getHistoryIndex(),
         local.get(), allowLocalChanges));

      // return null as we never used result
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public int size()
   {
      return parentCache.size();
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            parentCache.start();
            return null;
         }
      };
      SecurityHelper.doPrivilegedAction(action);
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            parentCache.stop();
            return null;
         }
      };
      SecurityHelper.doPrivilegedAction(action);
   }

   /**
    * {@inheritDoc}
    */
   public void addListener(Object listener)
   {
      parentCache.addListener(listener);
   }

   /**
    * {@inheritDoc}
    */
   public Set<Object> getListeners()
   {
      return parentCache.getListeners();
   }

   /**
    * {@inheritDoc}
    */
   public void removeListener(Object listener)
   {
      parentCache.removeListener(listener);
   }

   /**
    * {@inheritDoc}
    */
   public NotifyingFuture<Object> getAsync(CacheKey key)
   {
      return parentCache.getAsync(key);
   }

   /**
    * Start buffering process.
    */
   public void beginTransaction()
   {
      changesList.set(new CompressedISPNChangesBuffer());
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
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      final TransactionManager tm = getTransactionManager();
      try
      {
         final List<ChangesContainer> containers = changesContainer.getSortedList();
         commitChanges(tm, containers);
      }
      finally
      {
         changesList.set(null);
         changesContainer = null;
      }
   }

   /**
    * @param tm
    * @param containers
    */
   private void commitChanges(TransactionManager tm, List<ChangesContainer> containers)
   {
      for (ChangesContainer cacheChange : containers)
      {
         boolean isTxCreated = false;
         try
         {
            if (cacheChange.isTxRequired() && tm != null && tm.getStatus() == Status.STATUS_NO_TRANSACTION)
            {
               // No tx exists so we create a new tx
               if (LOG.isTraceEnabled())
                  LOG.trace("No Tx is active we then create a new tx");
               tm.begin();
               isTxCreated = true;
            }
         }
         catch (Exception e)
         {
            LOG.warn("Could not create a new tx", e);
         }
         try
         {
            cacheChange.apply();
         }
         catch (RuntimeException e)
         {
            if (isTxCreated)
            {
               try
               {
                  if (LOG.isTraceEnabled())
                     LOG.trace("An error occurs the tx will be rollbacked");
                  tm.rollback();
               }
               catch (Exception e1)
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
                  LOG.trace("The tx will be committed");
               tm.commit();
            }
            catch (Exception e)
            {
               LOG.warn("Could not commit the tx", e);
            }
         }
      }
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
      // start local transaction
      if (local && changesList.get() == null)
      {
         beginTransaction();
      }
      if (!local && this.local.get())
      {

      }
      this.local.set(local);
   }

   /**
    * Tries to get buffer and if it is null throws an exception otherwise returns buffer.
    * 
    * @return
    */
   private CompressedISPNChangesBuffer getChangesBufferSafe()
   {
      CompressedISPNChangesBuffer changesContainer = changesList.get();
      if (changesContainer == null)
      {
         throw new IllegalStateException("changesContainer should not be empty");
      }
      return changesContainer;
   }

   public TransactionManager getTransactionManager()
   {
      return parentCache.getTransactionManager();
   }

   /**
    * 
    * @param key
    * @param value
    */
   public void addToList(CacheKey key, Object value, boolean forceModify)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new AddToListContainer(key, value, parentCache, forceModify, changesContainer
         .getHistoryIndex(), local.get(), allowLocalChanges));
   }

   /**
    * 
    * @param string
    * @param node
    * @return
    */
   public Object putInBuffer(CacheKey key, Object value)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();

      // take Object from buffer for first 
      Object prevObject = getObjectFromChangesContainer(changesContainer, key);

      changesContainer.add(new PutObjectContainer(key, value, parentCache, changesContainer.getHistoryIndex(), local
         .get(), allowLocalChanges));

      if (prevObject != null)
      {
         return prevObject;
      }
      else
      {
         return parentCache.get(key);
      }
   }

   private Object getObjectFromChangesContainer(CompressedISPNChangesBuffer changesContainer, CacheKey key)
   {
      List<ChangesContainer> changes = changesContainer.getSortedList();
      Object object = null;

      for (ChangesContainer change : changes)
      {
         if (change.getChangesType().equals(ChangesType.PUT) && change.getKey().equals(key))
         {
            object = ((PutObjectContainer)change).value;
         }
      }

      return object;
   }

   /**
    * 
    * @param key
    * @param value
    */
   public void removeFromList(CacheKey key, Object value)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveFromListContainer(key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get(), allowLocalChanges));
   }

   public void removeFromPatternList(CacheKey key, ItemData value)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new RemoveFromPatternListContainer(key, value, parentCache, changesContainer
         .getHistoryIndex(), local.get(), allowLocalChanges));
   }

   public void addToPatternList(CacheKey key, ItemData value)
   {
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();
      changesContainer.add(new AddToPatternListContainer(key, value, parentCache, changesContainer.getHistoryIndex(),
         local.get(), allowLocalChanges));
   }

   public Object getFromBuffer(CacheKey key)
   {
      //look at buffer for first
      CompressedISPNChangesBuffer changesContainer = getChangesBufferSafe();

      Object objectFromBuffer = getObjectFromChangesContainer(changesContainer, key);

      if (objectFromBuffer != null)
      {
         return objectFromBuffer;
      }
      else
      {
         return parentCache.get(key);
      }
   }
}
