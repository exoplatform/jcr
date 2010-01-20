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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by The eXo Platform SAS.<br/>
 * 
 * This cache implementation store item data and childs lists of item data. And it implements
 * OBJECTS cache - i.e. returns same java object that was cached before. Same item data or list of
 * childs will be returned from getXXX() calls.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: LinkedWorkspaceStorageCacheImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class LinkedWorkspaceStorageCacheImpl implements WorkspaceStorageCache
{

   /**
    * Maximum cache size.
    */
   public static final int MAX_CACHE_SIZE = 2048; // 2

   /**
    * Maximum cache luve time in milliseconds.
    */
   public static final long MAX_CACHE_LIVETIME = 600 * 1000; // 10

   /**
    * Default statistic gathering period.
    */
   public static final long DEF_STATISTIC_PERIOD = 5 * 60000; // 5

   /**
    * Default cache C cleaner period.
    */
   public static final long DEF_CLEANER_PERIOD = 20 * 60000; // 20

   /**
    * Default blocking users amount.
    */
   public static final int DEF_BLOCKING_USERS_COUNT = 0;

   /**
    * TODO remove it
    */
   public static final String DEEP_DELETE_PARAMETER_NAME = "deep-delete";

   /**
    * statistic-period parameter name.
    */
   public static final String STATISTIC_PERIOD_PARAMETER_NAME = "statistic-period";

   /**
    * statistic-clean parameter name.
    */
   public static final String STATISTIC_CLEAN_PARAMETER_NAME = "statistic-clean";

   /**
    * statistic-log parameter name.
    */
   public static final String STATISTIC_LOG_PARAMETER_NAME = "statistic-log";

   /**
    * blocking-users-count parameter name.
    */
   public static final String BLOCKING_USERS_COUNT_PARAMETER_NAME = "blocking-users-count";

   /**
    * cleaner-period parameter name.
    */
   public static final String CLEANER_PERIOD_PARAMETER_NAME = "cleaner-period";

   /**
    * Default cache C load factor (HashMap parameter).
    */
   public static final float LOAD_FACTOR = 0.7f;

   /**
    * Cache implementaion logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("jcr.LinkedWorkspaceStorageCacheImpl");

   /**
    * Cache C.
    */
   private final Map<CacheKey, CacheValue> cache;

   /**
    * Cache C lock for concurrent usage.
    */
   private final CacheLock writeLock = new CacheLock();

   /**
    * Cache CN (Child nodes cache).
    */
   private final WeakHashMap<String, List<NodeData>> nodesCache;

   /**
    * Cache CP (Child properties).
    */
   private final WeakHashMap<String, List<PropertyData>> propertiesCache;

   /**
    * Cache name.
    */
   private final String name;

   /**
    * Enable flag.
    */
   private boolean enabled;

   /**
    * Worker timer (statistics and cleaner).
    */
   private final Timer workerTimer;

   /**
    * Last gathring cache statistic.
    */
   private CacheStatistic statistic;

   /**
    * Tell if we haveto remove whole node subtree (true), or just remove cached childs lists (false).
    * <br/> If true - it's more expensive operation.
    */
   private final boolean deepDelete;

   /**
    * Cache C item live time.
    */
   private long liveTime;

   /**
    * Cache C maximum size.
    */
   private final int maxSize;

   /**
    * Statistic miss current value.
    */
   private volatile long miss = 0;

   /**
    * Statistic hits current value.
    */
   private volatile long hits = 0;

   /**
    * Statistic totalGetTime current value.
    */
   private volatile long totalGetTime = 0;

   /**
    * cleanStatistics configuration.
    */
   private final boolean cleanStatistics;

   /**
    * showStatistic configuration.
    */
   private final boolean showStatistic;

   /**
    * Cache C lock class.
    */
   class CacheLock extends ReentrantLock
   {

      Collection<Thread> getLockThreads()
      {
         return getQueuedThreads();
      }

      Thread getLockOwner()
      {
         return getOwner();
      }
   }

   /**
    * Cache C map impl.
    */
   class CacheMap<K extends CacheKey, V extends CacheValue> extends LinkedHashMap<K, V>
   {

      CacheMap(long maxSize, float loadFactor)
      {
         super(Math.round(maxSize / loadFactor) + 100, loadFactor);
      }

      @Override
      protected boolean removeEldestEntry(final Entry<K, V> eldest)
      {
         if (size() > maxSize)
         {
            // remove with subnodes
            final CacheValue v = eldest.getValue();
            if (v != null)
            {
               final ItemData item = v.getItem();
               if (item.isNode())
               {
                  // removing childs of the node
                  nodesCache.remove(item.getIdentifier());
                  propertiesCache.remove(item.getIdentifier());
               }
            }

            return true;
         }
         else
            return false;
      }
   }

   /**
    * Cache C map uses blocking on get operation.
    */
   class BlockingCacheMap<K extends CacheKey, V extends CacheValue> extends CacheMap<K, V>
   {

      private final CacheLock userLock = new CacheLock();

      /**
       * Single user cache map.
       * 
       * @param maxSize
       * @param loadFactor
       */
      BlockingCacheMap(long maxSize, float loadFactor)
      {
         super(maxSize, loadFactor);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean containsValue(Object value)
      {
         userLock.lock();
         try
         {
            return super.containsValue(value);
         }
         finally
         {
            userLock.unlock();
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public V get(Object key)
      {
         userLock.lock();
         try
         {
            return super.get(key);
         }
         finally
         {
            userLock.unlock();
         }
      }
   }

   /**
    * Cache C map uses blocking on get operation.
    */
   class GroupBlockingCacheMap<K extends CacheKey, V extends CacheValue> extends CacheMap<K, V>
   {

      private final Semaphore usersLock;

      /**
       * Cache map allowes a limited set of concurrent users.
       * 
       * @param maxSize
       * @param loadFactor
       * @param limit
       */
      GroupBlockingCacheMap(long maxSize, float loadFactor, int limit)
      {
         super(maxSize, loadFactor);
         this.usersLock = new Semaphore(limit);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean containsValue(Object value)
      {
         try
         {
            usersLock.acquire();
         }
         catch (InterruptedException e)
         {
            LOG.warn("Error in cache.containsValue, current thread is interrupted.", e);
            return false;
         }

         try
         {
            return super.containsValue(value);
         }
         finally
         {
            usersLock.release();
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public V get(Object key)
      {
         try
         {
            usersLock.acquire();
         }
         catch (InterruptedException e)
         {
            LOG.warn("Error in cache.get, return null, current thread is interrupted.", e);
            return null;
         }

         try
         {
            return super.get(key);
         }
         finally
         {
            usersLock.release();
         }
      }
   }

   /**
    * Cache worker base class.
    */
   abstract class Worker extends Thread
   {
      protected volatile boolean done = false;

      Worker()
      {
         setDaemon(true);
      }
   }

   /**
    * Cache cleaner.
    */
   class Cleaner extends Worker
   {
      private final Log log;

      Cleaner(Log log)
      {
         this.log = log;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {
         try
         {
            cleanExpired();
         }
         finally
         {
            done = true;
         }
      }

      /**
       * Remove expired cache C records.
       */
      private void cleanExpired()
      {
         if (writeLock.tryLock())
         { // lock writers (putItem())
            String lockOwnerId = "";
            try
            {
               lockOwnerId = String.valueOf(writeLock.getLockOwner());
               int sizeBefore = cache.size();
               int expiredCount = 0;

               long start = System.currentTimeMillis();

               // WARN: cache map must be non modified during the operation,
               // writeLock care about this
               for (Iterator<Map.Entry<CacheKey, CacheValue>> citer = cache.entrySet().iterator(); citer.hasNext();)
               {
                  Map.Entry<CacheKey, CacheValue> ce = citer.next();
                  if (ce.getValue().getExpiredTime() <= System.currentTimeMillis())
                  {
                     ItemData item = ce.getValue().getItem();
                     if (item != null)
                        removeExpiredChilds(item);

                     citer.remove();
                     expiredCount++;
                  }
               }

               if (log.isDebugEnabled())
                  log.debug("Cleaner task done in " + (System.currentTimeMillis() - start) + "ms. Size " + sizeBefore
                     + " -> " + cache.size() + ", " + expiredCount + " processed.");
            }
            catch (ConcurrentModificationException e)
            {
               if (log.isDebugEnabled())
               {
                  StringBuilder lockUsers = new StringBuilder();
                  for (Thread user : writeLock.getLockThreads())
                  {
                     lockUsers.append(user.toString());
                     lockUsers.append(',');
                  }
                  log.error("Cleaner task error, cache in use. On-write owner [" + lockOwnerId + "], users ["
                     + lockUsers.toString() + "], error " + e, e);
               } // else it's not matter for work, the task will try next time
            }
            catch (Throwable e)
            {
               if (log.isDebugEnabled())
                  log.error("Cleaner task error " + e, e);
               else
                  log.error("Cleaner task error " + e + ". Will try next time.");
            }
            finally
            {
               writeLock.unlock();
            }
         }
         else // skip if lock is used by another process
         if (log.isDebugEnabled())
            log.debug("Cleaner task skipped. Ceche in use by another process ["
               + String.valueOf(writeLock.getLockOwner()) + "]. Will try next time.");
      }

      /**
       * Remove child items of the expired one.
       * 
       * @param item
       */
      private void removeExpiredChilds(final ItemData item)
      {
         if (item.isNode())
         {
            // remove this node properties list
            if (propertiesCache.remove(item.getIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeExpiredChilds() propertiesCache.remove " + item.getIdentifier());
            }
         }
         else
         {
            // removing properties list of the property parent
            if (propertiesCache.remove(item.getParentIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeExpiredChilds() propertiesCache.remove " + item.getParentIdentifier());
            }
         }
      }
   }

   /**
    * Statistics collector.
    */
   class StatisticCollector extends Worker
   {
      /*
       * (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      @Override
      public void run()
      {
         try
         {
            gatherStatistic();
         }
         finally
         {
            done = true;
         }
      }
   }

   /**
    * Base worker task calss.
    */
   abstract class WorkerTask extends TimerTask
   {
      protected Log log = ExoLogger.getLogger("jcr.LinkedWorkspaceStorageCacheImpl_Worker");

      protected Worker currentWorker = null;
   }

   /**
    * Cleaner task class.
    */
   class CleanerTask extends WorkerTask
   {
      public void run()
      {
         if (currentWorker == null || currentWorker.done)
         {
            // start worker and go out
            currentWorker = new Cleaner(log);
            currentWorker.start();
         }
         else // skip if previous in progress
         if (log.isDebugEnabled())
            log.debug("Cleaner task skipped. Previous one still runs. Will try next time.");
      }
   }

   /**
    * Gathering statistic task.
    */
   class StatisticTask extends WorkerTask
   {
      public void run()
      {
         if (currentWorker == null || currentWorker.done)
         {
            // start worker and go out
            currentWorker = new StatisticCollector();
            currentWorker.start();
         }
         else // skip if previous in progress
         if (log.isDebugEnabled())
            log.debug("Statistic task skipped. Previous one still runs. Will try next time.");
      }
   }

   /**
    * For debug/testing.
    * 
    * @param name
    * @param enabled
    * @param maxSize
    * @param cleanStatistics
    * @param blockingUsers
    * @param showStatistic
    * @param liveTime
    * @throws RepositoryConfigurationException
    */
   public LinkedWorkspaceStorageCacheImpl(String name, boolean enabled, int maxSize, long liveTimeSec,
      long cleanerPeriodMillis, long statisticPeriodMillis, boolean deepDelete, boolean cleanStatistics,
      int blockingUsers, boolean showStatistic) throws RepositoryConfigurationException
   {
      this.name = name;

      this.maxSize = maxSize;
      this.liveTime = liveTimeSec * 1000; // seconds
      this.nodesCache = new WeakHashMap<String, List<NodeData>>();
      this.propertiesCache = new WeakHashMap<String, List<PropertyData>>();
      this.enabled = enabled;
      this.deepDelete = deepDelete;
      this.cleanStatistics = cleanStatistics;

      this.cache = createCacheMap(blockingUsers);

      this.workerTimer = new Timer(this.name + "_CacheWorker", true);

      scheduleTask(new CleanerTask(), 5, cleanerPeriodMillis); // start after 5 sec

      this.showStatistic = showStatistic;

      gatherStatistic();
      scheduleTask(new StatisticTask(), 5, statisticPeriodMillis); // start after 5 sec
   }

   /**
    * Create cache using container configuration entry.
    * 
    * @param wsConfig
    *          workspace configuration
    * @throws RepositoryConfigurationException
    *           in case of missconfiguration
    */
   public LinkedWorkspaceStorageCacheImpl(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {

      this.name = "jcr." + wsConfig.getUniqueName();

      CacheEntry cacheConfig = wsConfig.getCache();

      long statisticPeriod;
      long cleanerPeriod;
      boolean cleanStats;
      int blockingUsers;
      boolean showStatistic;

      if (cacheConfig != null)
      {
         this.enabled = cacheConfig.isEnabled();

         int maxSizeConf;
         try
         {
            maxSizeConf = cacheConfig.getParameterInteger(MAX_SIZE_PARAMETER_NAME);
         }
         catch (RepositoryConfigurationException e)
         {
            maxSizeConf = cacheConfig.getParameterInteger("maxSize");
         }
         this.maxSize = maxSizeConf;

         int initialSize = maxSize > MAX_CACHE_SIZE ? maxSize / 4 : maxSize;
         this.nodesCache = new WeakHashMap<String, List<NodeData>>(initialSize, LOAD_FACTOR);
         this.propertiesCache = new WeakHashMap<String, List<PropertyData>>(initialSize, LOAD_FACTOR);

         try
         {
            // apply in milliseconds
            this.liveTime = cacheConfig.getParameterTime(LIVE_TIME_PARAMETER_NAME);
         }
         catch (RepositoryConfigurationException e)
         {
            this.liveTime = cacheConfig.getParameterTime("liveTime");
         }

         this.deepDelete = cacheConfig.getParameterBoolean(DEEP_DELETE_PARAMETER_NAME, false);

         blockingUsers = cacheConfig.getParameterInteger(BLOCKING_USERS_COUNT_PARAMETER_NAME, DEF_BLOCKING_USERS_COUNT);

         cleanerPeriod = cacheConfig.getParameterTime(CLEANER_PERIOD_PARAMETER_NAME, DEF_CLEANER_PERIOD);

         cleanStats = cacheConfig.getParameterBoolean(STATISTIC_CLEAN_PARAMETER_NAME, true);
         statisticPeriod = cacheConfig.getParameterTime(STATISTIC_PERIOD_PARAMETER_NAME, DEF_STATISTIC_PERIOD);
         showStatistic = cacheConfig.getParameterBoolean(STATISTIC_LOG_PARAMETER_NAME, false);

      }
      else
      {
         this.maxSize = MAX_CACHE_SIZE;
         this.liveTime = MAX_CACHE_LIVETIME;
         this.nodesCache = new WeakHashMap<String, List<NodeData>>();
         this.propertiesCache = new WeakHashMap<String, List<PropertyData>>();
         this.enabled = true;
         this.deepDelete = false;

         blockingUsers = DEF_BLOCKING_USERS_COUNT;

         statisticPeriod = DEF_STATISTIC_PERIOD;
         cleanerPeriod = DEF_CLEANER_PERIOD;
         cleanStats = true;
         showStatistic = false;
      }

      this.cleanStatistics = cleanStats;
      this.showStatistic = showStatistic;// name

      if (blockingUsers > 2048)
      {
         // limit it with 2k
         blockingUsers = 2048;
         LOG.warn(BLOCKING_USERS_COUNT_PARAMETER_NAME + " maximum is limited to 2k. Using " + blockingUsers);
      }

      this.cache = createCacheMap(blockingUsers);

      this.workerTimer = new Timer(this.name + "_CacheWorker", true);

      // cleaner
      int start = ((int)cleanerPeriod) / 2; // half or period
      start = start > 60000 ? start : 60000; // don't start now
      scheduleTask(new CleanerTask(), start, cleanerPeriod);

      // statistic collector
      gatherStatistic();
      start = ((int)statisticPeriod) / 2; // half or period
      start = start > 15000 ? start : 15000; // don't start now
      scheduleTask(new StatisticTask(), start, statisticPeriod);
   }

   private Map<CacheKey, CacheValue> createCacheMap(int blockingUsers)
   {
      if (blockingUsers <= 0)
      {
         // full access cache
         LOG.info(this.name + " Create unblocking cache map.");
         return new CacheMap<CacheKey, CacheValue>(maxSize, LOAD_FACTOR);
      }
      else if (blockingUsers == 1)
      {
         // per user locked cache (get-lock)
         LOG.info(this.name + " Create per-user blocking cache map.");
         return new BlockingCacheMap<CacheKey, CacheValue>(maxSize, LOAD_FACTOR);
      }
      else
      {
         // per users (count) locked cache (get-locks)
         LOG.info(this.name + " Create per-users-group blocking cache map.");
         return new GroupBlockingCacheMap<CacheKey, CacheValue>(maxSize, LOAD_FACTOR, blockingUsers);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         workerTimer.cancel();
      }
      catch (Throwable e)
      {
         System.err.println(this.name + " cache, finalyze error " + e);
      }

      nodesCache.clear();
      propertiesCache.clear();
      cache.clear();

      super.finalize();
   }

   private void scheduleTask(TimerTask task, int start, long period)
   {
      Calendar firstTime = Calendar.getInstance();
      firstTime.add(Calendar.MILLISECOND, start);

      if (period < 30000)
      {
         // warn and use 30sec.
         LOG.warn("Cache worker schedule period too short " + period + ". Will use 30sec.");
         period = 30000;
      }

      workerTimer.schedule(task, firstTime.getTime(), period);
   }

   private void gatherStatistic()
   {
      final CacheStatistic st =
         new CacheStatistic(miss, hits, cache.size(), nodesCache.size(), propertiesCache.size(), maxSize, liveTime,
            totalGetTime);

      if (showStatistic)
         try
         {
            double rel =
               st.getMiss() > 0 && st.getHits() > 0 ? (Math.round((10000d * st.getHits()) / st.getMiss())) / 10000d : 0;
            LOG.info("Cache "
               + name
               + ": relevancy "
               + rel
               + " (hits:"
               + st.getHits()
               + ", miss:"
               + st.getMiss()
               + "), get:"
               + Math.round((st.getHits() + st.getMiss())
                  / (st.getTotalGetTime() > 0 ? st.getTotalGetTime() / 1000d : 1)) + "oper/sec ("
               + (st.getTotalGetTime() / 1000d) + "sec)" + ", size:" + st.getSize() + " (max " + st.getMaxSize() + ")"
               + ", childs(nodes:" + st.getNodesSize() + ", properties:" + st.getPropertiesSize() + ")");
         }
         catch (Throwable e)
         {
            LOG.warn("Show statistic log.info error " + e);
         }

      this.statistic = st;

      if (cleanStatistics)
      {
         miss = 0;
         hits = 0;
         totalGetTime = 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getSize()
   {
      return cache.size();
   }

   /**
    * getName.
    * 
    * @return
    */
   public String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(final String identifier)
   {
      if (enabled && identifier != null)
      {
         try
         {
            return getItem(identifier);
         }
         catch (Exception e)
         {
            LOG.error("GET operation fails. Item ID=" + identifier + ". Error " + e + ". NULL returned.", e);
         }
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(final String parentId, final QPathEntry name)
   {
      if (enabled && parentId != null && name != null)
      {
         try
         {
            return getItem(parentId, name);
         }
         catch (Exception e)
         {
            LOG.error("GET operation fails. Parent ID=" + parentId + " name "
               + (name != null ? name.getAsString() : name) + ". Error " + e + ". NULL returned.", e);
         }
      }

      return null;
   }

   /**
    * Put item in cache C.
    * 
    * @param data
    */
   protected void putItem(final ItemData data)
   {
      cache.put(new CacheId(data.getIdentifier()), new CacheValue(data, System.currentTimeMillis() + liveTime));
      cache.put(new CacheQPath(data.getParentIdentifier(), data.getQPath()), new CacheValue(data, System
         .currentTimeMillis()
         + liveTime));
   }

   /**
    * {@inheritDoc}
    */
   public void put(final ItemData item)
   {
      if (enabled && item != null)
      {

         writeLock.lock();
         try
         {
            if (LOG.isDebugEnabled())
               LOG.debug(name + ", put()    " + item.getQPath().getAsString() + "    " + item.getIdentifier()
                  + "  --  " + item);

            putItem(item);

            // add child item data to list of childs of the parent
            if (item.isNode())
            {
               // add child node
               List<NodeData> cachedParentChilds = nodesCache.get(item.getParentIdentifier());
               if (cachedParentChilds != null)
               {
                  // Playing for orderable work
                  NodeData nodeData = (NodeData)item;
                  int orderNumber = nodeData.getOrderNumber();

                  synchronized (cachedParentChilds)
                  {
                     int index = cachedParentChilds.indexOf(nodeData);
                     if (index >= 0)
                     {

                        if (orderNumber != cachedParentChilds.get(index).getOrderNumber())
                        {
                           // replace and reorder
                           List<NodeData> newChilds = new ArrayList<NodeData>(cachedParentChilds.size());
                           for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                           {
                              if (index == ci)
                                 newChilds.add(nodeData); // place in new position
                              else
                                 newChilds.add(cachedParentChilds.get(ci)); // copy
                           }

                           nodesCache.put(item.getParentIdentifier(), newChilds); // cache
                           // new
                           // list
                           if (LOG.isDebugEnabled())
                              LOG.debug(name + ", put()    update child node  " + nodeData.getIdentifier()
                                 + "  order #" + orderNumber);
                        }
                        else
                        {

                           cachedParentChilds.set(index, nodeData); // replace at
                           // current position
                           if (LOG.isDebugEnabled())
                              LOG.debug(name + ", put()    update child node  " + nodeData.getIdentifier()
                                 + "  at index #" + index);
                        }

                     }
                     else
                     {

                        // add new to the end
                        List<NodeData> newChilds = new ArrayList<NodeData>(cachedParentChilds.size() + 1);
                        for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                           newChilds.add(cachedParentChilds.get(ci));

                        newChilds.add(nodeData); // add

                        nodesCache.put(item.getParentIdentifier(), newChilds); // cache new list
                        if (LOG.isDebugEnabled())
                           LOG.debug(name + ", put()    add child node  " + nodeData.getIdentifier());
                     }
                  }
               }
            }
            else
            {
               // add child property
               final List<PropertyData> cachedParentChilds = propertiesCache.get(item.getParentIdentifier());
               if (cachedParentChilds != null)
               {
                  if (cachedParentChilds.get(0).getValues().size() > 0)
                  {
                     synchronized (cachedParentChilds)
                     {
                        // if it's a props list with values, update it
                        int index = cachedParentChilds.indexOf(item);
                        if (index >= 0)
                        {
                           // update already cached in list
                           cachedParentChilds.set(index, (PropertyData)item); // replace at current position
                           if (LOG.isDebugEnabled())
                              LOG.debug(name + ", put()    update child property  " + item.getIdentifier()
                                 + "  at index #" + index);

                        }
                        else if (index == -1)
                        {
                           // add new
                           List<PropertyData> newChilds = new ArrayList<PropertyData>(cachedParentChilds.size() + 1);
                           for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                              newChilds.add(cachedParentChilds.get(ci));

                           newChilds.add((PropertyData)item);
                           propertiesCache.put(item.getParentIdentifier(), newChilds); // cache new list
                           if (LOG.isDebugEnabled())
                              LOG.debug(name + ", put()    add child property  " + item.getIdentifier());
                        }
                     }
                  }
                  else
                     // if it's a props list with empty values, remove cached list
                     propertiesCache.remove(item.getParentIdentifier());
               }
            }
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error put item data in cache: "
               + (item != null ? item.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            writeLock.unlock();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildProperties(final NodeData parentData, final List<PropertyData> childItems)
   {
      if (enabled && parentData != null && childItems != null)
      { // TODO don't check parentData !=
         // null && childItems != null

         String logInfo = null;
         if (LOG.isDebugEnabled())
         {
            logInfo =
               "parent:   " + parentData.getQPath().getAsString() + "    " + parentData.getIdentifier() + " "
                  + childItems.size();
            LOG.debug(name + ", addChildProperties() >>> " + logInfo);
         }

         String operName = ""; // for debug/trace only

         writeLock.lock();
         try
         {
            // remove parent (no childs)
            operName = "removing parent";
            removeItem(parentData);

            operName = "caching parent";
            putItem(parentData); // put parent in cache

            synchronized (childItems)
            {
               operName = "caching child properties list";
               propertiesCache.put(parentData.getIdentifier(), childItems); // put childs in cache CP

               operName = "caching child properties";
               // put childs in cache C
               for (ItemData p : childItems)
               {
                  if (LOG.isDebugEnabled())
                     LOG.debug(name + ", addChildProperties()    " + p.getQPath().getAsString() + "    "
                        + p.getIdentifier() + "  --  " + p);

                  putItem(p);
               }
            }
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in addChildProperties() " + operName + ": parent "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            writeLock.unlock();
         }

         if (LOG.isDebugEnabled())
            LOG.debug(name + ", addChildProperties() <<< " + logInfo);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildPropertiesList(final NodeData parentData, final List<PropertyData> childItems)
   {
      if (enabled && parentData != null && childItems != null)
      { // TODO don't check parentData !=
         // null && childItems != null

         String logInfo = null;
         if (LOG.isDebugEnabled())
         {
            logInfo =
               "parent:   " + parentData.getQPath().getAsString() + "    " + parentData.getIdentifier() + " "
                  + childItems.size();
            LOG.debug(name + ", addChildPropertiesList() >>> " + logInfo);
         }

         String operName = ""; // for debug/trace only

         writeLock.lock();
         try
         {
            // remove parent (no childs)
            operName = "removing parent";
            removeItem(parentData);

            operName = "caching parent";
            putItem(parentData); // put parent in cache

            synchronized (childItems)
            {
               operName = "caching child properties list";
               propertiesCache.put(parentData.getIdentifier(), childItems); // put childs in cache CP
            }
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in addChildPropertiesList() " + operName + ": parent "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            writeLock.unlock();
         }

         if (LOG.isDebugEnabled())
            LOG.debug(name + ", addChildPropertiesList() <<< " + logInfo);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodes(final NodeData parentData, final List<NodeData> childItems)
   {
      if (enabled && parentData != null && childItems != null)
      { // TODO don't check parentData !=
         // null && childItems != null

         String logInfo = null;
         if (LOG.isDebugEnabled())
         {
            logInfo =
               "parent:   " + parentData.getQPath().getAsString() + "    " + parentData.getIdentifier() + " "
                  + childItems.size();
            LOG.debug(name + ", addChildNodes() >>> " + logInfo);
         }

         String operName = ""; // for debug/trace only

         writeLock.lock();
         try
         {
            // remove parent (no childs)
            operName = "removing parent";
            removeItem(parentData);

            operName = "caching parent";
            putItem(parentData); // put parent in cache

            synchronized (childItems)
            {
               operName = "caching child nodes list";
               nodesCache.put(parentData.getIdentifier(), childItems); // put childs
               // in cache CN

               operName = "caching child nodes";
               // put childs in cache C
               for (ItemData n : childItems)
               {
                  if (LOG.isDebugEnabled())
                     LOG.debug(name + ", addChildNodes()    " + n.getQPath().getAsString() + "    " + n.getIdentifier()
                        + "  --  " + n);

                  putItem(n);
               }
            }
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in addChildNodes() " + operName + ": parent "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            writeLock.unlock();
         }

         if (LOG.isDebugEnabled())
            LOG.debug(name + ", addChildNodes() <<< " + logInfo);
      }
   }

   /**
    * Removes data and its children in cache.<br>
    * Implementation details<br>
    * Remove Item from cache C, for Node removes lists in CN and CP (only lists).<br>
    * Remove Item from parent's child lists (CN for Node, CP for Property). NOTE: if CN or CP of the
    * Item parent are iterrating now ConcurrentModificationException will occurs there. NOTE #2: used
    * from onSaveItems().
    * 
    * @param item
    */
   public void remove(final ItemData item)
   {
      if (enabled && item != null)
      {
         if (LOG.isDebugEnabled())
            LOG.debug(name + ", remove() " + item.getQPath().getAsString() + " " + item.getIdentifier());

         writeLock.lock();
         try
         {
            final String itemId = item.getIdentifier();

            removeItem(item);

            if (item.isNode())
            {
               // removing childs of the node
               nodesCache.remove(itemId);
               propertiesCache.remove(itemId);

               // removing child from the node's parent child nodes list
               removeChildNode(item.getParentIdentifier(), itemId);
            }
            else
               removeChildProperty(item.getParentIdentifier(), itemId);
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error remove item data from cache: " + item.getQPath().getAsString(), e);
         }
         finally
         {
            writeLock.unlock();
         }
      }
   }

   /**
    * Get item from cache C by item id. Checks is it expired, calcs statistics.
    * 
    * @param identifier
    * @return item
    */
   protected ItemData getItem(final String identifier)
   {
      long start = System.currentTimeMillis();
      try
      {
         final CacheId k = new CacheId(identifier);
         final CacheValue v = cache.get(k);
         if (v != null)
         {
            final ItemData c = v.getItem();

            if (v.getExpiredTime() > System.currentTimeMillis())
            {
               // check if wasn't removed
               if (LOG.isDebugEnabled())
                  LOG.debug(name + ", getItem() " + identifier + " --> "
                     + (c != null ? c.getQPath().getAsString() + " parent:" + c.getParentIdentifier() : "[null]"));

               hits++;
               return c;
            }

            // remove expired
            writeLock.lock();
            try
            {
               cache.remove(k);

               // remove by parentId + path
               cache.remove(new CacheQPath(c.getParentIdentifier(), c.getQPath()));

               // remove cached child lists
               if (c.isNode())
               {
                  nodesCache.remove(c.getIdentifier());
                  propertiesCache.remove(c.getIdentifier());
               }
            }
            finally
            {
               writeLock.unlock();
            }
         }

         miss++;
         return null;
      }
      finally
      {
         totalGetTime += System.currentTimeMillis() - start;
      }
   }

   /**
    * Get item from cache C by item parent and name. Checks is it expired, calcs statistics.
    * 
    * @param key
    *          a InternalQPath path of item cached
    */
   protected ItemData getItem(final String parentUuid, final QPathEntry qname)
   {
      long start = System.currentTimeMillis();
      try
      {
         final CacheQPath k = new CacheQPath(parentUuid, qname);
         final CacheValue v = cache.get(k);
         if (v != null)
         {
            final ItemData c = v.getItem();

            if (v.getExpiredTime() > System.currentTimeMillis())
            {
               if (LOG.isDebugEnabled())
                  LOG.debug(name + ", getItem() " + (c != null ? c.getQPath().getAsString() : "[null]") + " --> "
                     + (c != null ? c.getIdentifier() + " parent:" + c.getParentIdentifier() : "[null]"));

               hits++;
               return c;
            }

            // remove expired
            writeLock.lock();
            try
            {
               cache.remove(k);

               // remove by Id
               cache.remove(new CacheId(c.getIdentifier()));

               // remove cached child lists
               if (c.isNode())
               {
                  nodesCache.remove(c.getIdentifier());
                  propertiesCache.remove(c.getIdentifier());
               }
            }
            finally
            {
               writeLock.unlock();
            }
         }

         miss++;
         return null;
      }
      finally
      {
         totalGetTime += System.currentTimeMillis() - start;
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodes(final NodeData parentData)
   {
      if (enabled && parentData != null)
      {
         long start = System.currentTimeMillis();
         try
         {
            // we assume that parent cached too
            final List<NodeData> cn = nodesCache.get(parentData.getIdentifier());

            if (LOG.isDebugEnabled())
            {
               LOG.debug(name + ", getChildNodes() " + parentData.getQPath().getAsString() + " "
                  + parentData.getIdentifier());
               final StringBuffer blog = new StringBuffer();
               if (cn != null)
               {
                  blog.append("\n");
                  for (NodeData nd : cn)
                  {
                     blog.append("\t\t" + nd.getQPath().getAsString() + " " + nd.getIdentifier() + "\n");
                  }
                  LOG.debug("\t-->" + blog.toString());
               }
               else
               {
                  LOG.debug("\t--> null");
               }
            }

            if (cn != null)
               hits++;
            else
               miss++;
            return cn;
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in getChildNodes() parentData: "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            totalGetTime += System.currentTimeMillis() - start;
         }
      }

      return null; // nothing cached
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildProperties(final NodeData parentData)
   {
      if (enabled && parentData != null)
      {
         long start = System.currentTimeMillis();
         try
         {
            // we assume that parent cached too
            final List<PropertyData> cp = propertiesCache.get(parentData.getIdentifier());

            if (LOG.isDebugEnabled())
            {
               LOG.debug(name + ", getChildProperties() " + parentData.getQPath().getAsString() + " "
                  + parentData.getIdentifier());
               final StringBuffer blog = new StringBuffer();
               if (cp != null)
               {
                  blog.append("\n");
                  for (PropertyData pd : cp)
                  {
                     blog.append("\t\t" + pd.getQPath().getAsString() + " " + pd.getIdentifier() + "\n");
                  }
                  LOG.debug("\t--> " + blog.toString());
               }
               else
               {
                  LOG.debug("\t--> null");
               }
            }

            if (cp != null && cp.get(0).getValues().size() > 0)
            {
               // don't return list of empty-valued props (but listChildProperties() can)
               hits++;
               return cp;
            }
            else
               miss++;
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in getChildProperties() parentData: "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         finally
         {
            totalGetTime += System.currentTimeMillis() - start;
         }
      }

      return null; // nothing cached
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildProperties(final NodeData parentData)
   {
      if (enabled && parentData != null)
      {
         try
         {
            // we assume that parent cached too
            final List<PropertyData> cp = propertiesCache.get(parentData.getIdentifier());

            if (LOG.isDebugEnabled())
            {
               LOG.debug(name + ", listChildProperties() " + parentData.getQPath().getAsString() + " "
                  + parentData.getIdentifier());
               final StringBuffer blog = new StringBuffer();
               if (cp != null)
               {
                  blog.append("\n");
                  for (PropertyData pd : cp)
                  {
                     blog.append("\t\t" + pd.getQPath().getAsString() + " " + pd.getIdentifier() + "\n");
                  }
                  LOG.debug("\t--> " + blog.toString());
               }
               else
               {
                  LOG.debug("\t--> null");
               }
            }

            if (cp != null)
               hits++;
            else
               miss++;
            return cp;
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error in listChildProperties() parentData: "
               + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
      }

      return null; // nothing cached
   }

   /**
    * {@inheritDoc}
    */
   public boolean isEnabled()
   {
      return enabled;
   }

   /**
    * Enable cache.
    * 
    * @param enabled
    */
   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }

   /**
    * Not supported now.
    * 
    * @param maxSize
    */
   public void setMaxSize(int maxSize)
   {
      // TODO not supported now, but it's possible as an option
      // e.g. we will create new cache instance with new size and fill it with
      // current cache size.
      // it's fully synchronized operation, i.e. method
      LOG.warn("setMaxSize not supported now");
   }

   /**
    * Set liveTime of newly cached items.
    * 
    * @param liveTime
    */
   public void setLiveTime(long liveTime)
   {
      writeLock.lock();
      try
      {
         this.liveTime = liveTime;
      }
      finally
      {
         writeLock.unlock();
      }
      LOG
         .info(name + " : set liveTime=" + liveTime + "ms. New value will be applied to items cached from this moment.");
   }

   /**
    * Remove item from cache C.
    * 
    * @param item
    */
   protected void removeItem(final ItemData item)
   {
      final String itemId = item.getIdentifier();

      cache.remove(new CacheId(itemId));

      final CacheValue v2 = cache.remove(new CacheQPath(item.getParentIdentifier(), item.getQPath()));
      if (v2 != null && !v2.getItem().getIdentifier().equals(itemId))
         // same path but diff identifier node... phantom
         removeItem(v2.getItem());
   }

   /**
    * Remove sibling's subtrees from cache C, CN, CP.<br/> For update (order-before) usecase.<br/>
    * The work does remove of all descendants of the item parent. I.e. the node and its siblings (for
    * SNS case).<br/>
    */
   protected void removeSiblings(final NodeData node)
   {
      if (node.getIdentifier().equals(Constants.ROOT_UUID))
         return;

      // remove child nodes of the item parent recursive
      writeLock.lock();
      try
      {
         // remove on-parent child nodes list
         nodesCache.remove(node.getParentIdentifier());

         // go through the C and remove every descendant of the node parent
         final QPath path = node.getQPath().makeParentPath();
         final List<CacheId> toRemove = new ArrayList<CacheId>();

         // find and remove by path
         for (Iterator<Map.Entry<CacheKey, CacheValue>> citer = cache.entrySet().iterator(); citer.hasNext();)
         {
            Map.Entry<CacheKey, CacheValue> ce = citer.next();
            CacheKey key = ce.getKey();
            CacheValue v = ce.getValue();
            if (v != null)
            {
               if (key.isDescendantOf(path))
               {
                  // will remove by id too
                  toRemove.add(new CacheId(v.getItem().getIdentifier()));

                  citer.remove(); // remove

                  nodesCache.remove(v.getItem().getIdentifier());
                  propertiesCache.remove(v.getItem().getIdentifier());
               }
            }
            else
               citer.remove(); // remove empty C record
         }

         for (CacheId id : toRemove)
            cache.remove(id);

         toRemove.clear();

      }
      finally
      {
         writeLock.unlock();
      }
   }

   // --------------------- ItemsPersistenceListener --------------

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(final ItemStateChangesLog changesLog)
   {

      if (!enabled)
         return;

      ItemState prevState = null;
      for (Iterator<ItemState> iter = changesLog.getAllStates().iterator(); iter.hasNext();)
      {
         ItemState state = iter.next();
         ItemData item = state.getData();
         if (LOG.isDebugEnabled())
            LOG.debug(name + ", onSaveItems() " + ItemState.nameFromValue(state.getState()) + " "
               + item.getQPath().getAsString() + " " + item.getIdentifier() + " parent:" + item.getParentIdentifier());

         try
         {
            if (state.isAdded())
            {
               put(item);
            }
            else if (state.isDeleted())
            {
               remove(item);
            }
            else if (state.isRenamed())
            {
               // MOVE operation (DESTENATION changes, same as ADDED), states for whole subtree!
               // RENAME goes before DELETE
               put(item);
            }
            else if (state.isUpdated())
            {
               // UPDATE occurs on reordered (no subtree!) and merged nodes (for each
               // merged-updated)
               if (item.isNode())
               {
                  if (prevState != null)
                  {
                     // play only for reorder, UPDATE goes after DELETE of same path
                     // item
                     // we have to unload node and its parent child nodes to be loaded
                     // back from the persistence
                     if (prevState.isDeleted()
                        && prevState.getData().getParentIdentifier().equals(item.getParentIdentifier()))
                        removeSiblings((NodeData)item);
                  }
               }
               else if (item.getQPath().getName().equals(Constants.EXO_PERMISSIONS))
               {
                  // TODO EXOJCR-12 place to put workaround for JCR cache exo:permissions updated
                  // get parent Node

                  // check if parent is mix:privilegeable
                  ItemData parent = get(item.getParentIdentifier());
                  // delete parent
                  remove(parent);

                  // delete parent containing child nodes list
                  nodesCache.remove(parent.getParentIdentifier());

                  // traverse itemCache
                  Iterator<CacheValue> cacheIterator = cache.values().iterator();
                  while (cacheIterator.hasNext())
                  {
                     ItemData cachedItem = cacheIterator.next().getItem();
                     if (cachedItem.isNode())
                     {
                        if (cachedItem.getQPath().isDescendantOf(parent.getQPath()))
                        {
                           cacheIterator.remove();
                        }
                     }
                  }

                  // traverse child node Cache
                  Iterator<List<NodeData>> childNodesIterator = nodesCache.values().iterator();
                  while (childNodesIterator.hasNext())
                  {
                     List<NodeData> list = childNodesIterator.next();
                     if (list != null && list.size() > 0)
                     {
                        if (list.get(0).getQPath().isDescendantOf(parent.getQPath()))
                        {
                           childNodesIterator.remove();
                        }
                     }
                  }
               }
               put(item);
            }
            else if (state.isMixinChanged())
            {
               // MIXIN_CHANGED, on Node
               put(item);
            }
         }
         catch (Exception e)
         {
            LOG.error(name + ", Error process onSaveItems action for item data: "
               + (item != null ? item.getQPath().getAsString() : "[null]"), e);
         }

         prevState = state;
      }
   }

   // ---------------------------------------------------

   /**
    * Remove property by id if parent properties are cached in CP.
    * 
    * @param parentIdentifier
    *          - parent id
    * @param childIdentifier
    *          - property id
    * @return removed property or null if property not cached or parent properties are not cached
    * @throws Exception
    */
   protected PropertyData removeChildProperty(final String parentIdentifier, final String childIdentifier)
   {
      final List<PropertyData> childProperties = propertiesCache.get(parentIdentifier);
      if (childProperties != null)
      {
         synchronized (childProperties)
         { // [PN] 17.01.07
            for (Iterator<PropertyData> i = childProperties.iterator(); i.hasNext();)
            {
               PropertyData cn = i.next();
               if (cn.getIdentifier().equals(childIdentifier))
               {
                  i.remove();
                  if (childProperties.size() <= 0)
                     propertiesCache.remove(parentIdentifier);
                  return cn;
               }
            }
         }
      }
      return null;
   }

   /**
    * Remove child node by id if parent child nodes are cached in CN.
    * 
    * @param parentIdentifier
    *          - parebt if
    * @param childIdentifier
    *          - node id
    * @return removed node or null if node not cached or parent child nodes are not cached
    * @throws Exception
    */
   protected NodeData removeChildNode(final String parentIdentifier, final String childIdentifier)
   {
      final List<NodeData> childNodes = nodesCache.get(parentIdentifier);
      if (childNodes != null)
      {
         synchronized (childNodes)
         { // [PN] 17.01.07
            for (Iterator<NodeData> i = childNodes.iterator(); i.hasNext();)
            {
               NodeData cn = i.next();
               if (cn.getIdentifier().equals(childIdentifier))
               {
                  i.remove();
                  return cn;
               }
            }
         }
      }
      return null;
   }

   /**
    * Return last gathered statistic.<br/>
    * 
    * @return CacheStatistic
    */
   public CacheStatistic getStatistic()
   {
      return statistic;
   }

   // -- for debug

   /**
    * For debug.
    */
   String dump()
   {
      StringBuilder res = new StringBuilder();
      for (Map.Entry<CacheKey, CacheValue> ce : cache.entrySet())
      {
         res.append(ce.getKey().hashCode());
         res.append("\t\t");
         res.append(ce.getValue().getItem().getIdentifier());
         res.append(", ");
         res.append(ce.getValue().getItem().getQPath().getAsString());
         res.append(", ");
         res.append(ce.getValue().getExpiredTime());
         res.append(", ");
         res.append(ce.getKey().getClass().getSimpleName());
         res.append("\r\n");
      }

      return res.toString();
   }

   public void beginTransaction()
   {
      // TODO Auto-generated method stub

   }

   public void commitTransaction()
   {
      // TODO Auto-generated method stub

   }

   public void rollbackTransaction()
   {
      // TODO Auto-generated method stub

   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }
}
