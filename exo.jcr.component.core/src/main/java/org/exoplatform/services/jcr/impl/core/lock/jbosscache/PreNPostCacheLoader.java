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
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.RegionManager;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.CacheLoader;
import org.jboss.cache.lock.TimeoutException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This {@link CacheLoader} is used to encapsulate the {@link CacheLoader} used to persist the data of the Locks.
 * This is used to prevent {@link TimeoutException} that occur when several threads try to access the same data
 * at the same time and the data is missing in the local cache which is the case most of the time, since no data
 * means that the node is not locked. This {@link CacheLoader} will then check if some data exists before calling
 * the main {@link CacheLoader} that is used to persist the data and if no data exist either in the local cache and
 * the main {@link CacheLoader}, this {@link CacheLoader} will load dummy data to indicate that the data has already
 * been loaded. 
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 4 févr. 2010  
 */
@SuppressWarnings("unchecked")
public class PreNPostCacheLoader implements CacheLoader
{
   /**
    * The configuration of the current cache loader
    */
   private IndividualCacheLoaderConfig config;

   /**
    * Indicates if this cache loader is called before the main cache loader of after the main cache loader
    */
   private boolean preCacheLoader;

   /**
    * The related cache
    */
   private CacheSPI cache;

   /**
    * The default constructor
    * @param preCacheLoader indicates if this cache loader must be added before the main cache loader or after it.
    * <code>true</code> means that it has been added before the main cache loader.
    */
   public PreNPostCacheLoader(boolean preCacheLoader)
   {
      this.preCacheLoader = preCacheLoader;
   }
   
   /**
    * @see org.jboss.cache.loader.CacheLoader#commit(java.lang.Object)
    */
   public void commit(Object tx) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#exists(org.jboss.cache.Fqn)
    */
   public boolean exists(Fqn name) throws Exception
   {
      if (preCacheLoader)
      {
         // Before calling the main cache loader we first check if the data exists in the local cache
         // in order to prevent multiple call to the cache store
         return cache.peek(name, false) != null;
      }
      else
      {
         // The main cache loader has been called but no data could find into the cache store 
         // so to prevent a multiple useless call, we return an empty map to enforce JBC to create
         // a node that will indicate that the data has already been loaded but no data was found
         return true;
      }      
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#get(org.jboss.cache.Fqn)
    */
   public Map<Object, Object> get(Fqn name) throws Exception
   {
      if (preCacheLoader)
      {
         // Before calling the main cache loader we first check if the data exists in the local cache
         // in order to prevent multiple call to the cache store
         NodeSPI<Object, Object> node = cache.peek(name, false);
         if (node != null)
         {
            // The node exists which means that the data has already been loaded, so we return the data
            // already loaded
            return node.getDataDirect();
         }
         else
         {
            // No data has been loaded, so we can call the main cache loader 
            return null;
         }
      }
      else
      {
         // The main cache loader has been called but no data could find into the cache store 
         // so to prevent a multiple useless call, we return an empty map to enforce JBC to create
         // a node that will indicate that the data has already been loaded but no data was found
         return Collections.emptyMap();
      }
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#getChildrenNames(org.jboss.cache.Fqn)
    */
   public Set<?> getChildrenNames(Fqn fqn) throws Exception
   {
      return null;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#getConfig()
    */
   public IndividualCacheLoaderConfig getConfig()
   {
      return config;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#loadEntireState(java.io.ObjectOutputStream)
    */
   public void loadEntireState(ObjectOutputStream os) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#loadState(org.jboss.cache.Fqn, java.io.ObjectOutputStream)
    */
   public void loadState(Fqn subtree, ObjectOutputStream os) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#prepare(java.lang.Object, java.util.List, boolean)
    */
   public void prepare(Object tx, List<Modification> modifications, boolean onePhase) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(java.util.List)
    */
   public void put(List<Modification> modifications) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.util.Map)
    */
   public void put(Fqn name, Map<Object, Object> attributes) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(Fqn name, Object key, Object value) throws Exception
   {
      return null;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#remove(org.jboss.cache.Fqn)
    */
   public void remove(Fqn fqn) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#remove(org.jboss.cache.Fqn, java.lang.Object)
    */
   public Object remove(Fqn fqn, Object key) throws Exception
   {
      return null;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#removeData(org.jboss.cache.Fqn)
    */
   public void removeData(Fqn fqn) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#rollback(java.lang.Object)
    */
   public void rollback(Object tx)
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#setCache(org.jboss.cache.CacheSPI)
    */
   public void setCache(CacheSPI cache)
   {
      this.cache = cache;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#setConfig(org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig)
    */
   public void setConfig(IndividualCacheLoaderConfig config)
   {
      this.config = config;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#setRegionManager(org.jboss.cache.RegionManager)
    */
   public void setRegionManager(RegionManager manager)
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#storeEntireState(java.io.ObjectInputStream)
    */
   public void storeEntireState(ObjectInputStream is) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#storeState(org.jboss.cache.Fqn, java.io.ObjectInputStream)
    */
   public void storeState(Fqn subtree, ObjectInputStream is) throws Exception
   {
   }

   /**
    * @see org.jboss.cache.Lifecycle#create()
    */
   public void create() throws Exception
   {
   }

   /**
    * @see org.jboss.cache.Lifecycle#destroy()
    */
   public void destroy()
   {
   }

   /**
    * @see org.jboss.cache.Lifecycle#start()
    */
   public void start() throws Exception
   {
   }

   /**
    * @see org.jboss.cache.Lifecycle#stop()
    */
   public void stop()
   {
   }
}
