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

import org.exoplatform.commons.utils.SecurityHelper;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.RegionManager;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.CacheLoader;
import org.jboss.cache.lock.TimeoutException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This {@link CacheLoader} is used to encapsulate the {@link CacheLoader} used to persist the data of the Locks.
 * This is used to prevent {@link TimeoutException} that occur when several threads try to access the same data
 * at the same time and the data is missing in the local cache which is the case most of the time, since no data
 * means that the node is not locked. Since all the lock data will be loaded at startup, this {@link CacheLoader} 
 * will only call the nested {@link CacheLoader} for read operations when the cache status is CacheStatus.STARTING,
 * for all other status, we don't call the nested cache loader since if no data cans be found in the local cache,
 * it means that there is no data to load because all the lock data has been loaded at startup. 
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 9 f�vr. 2010  
 */
@SuppressWarnings("unchecked")
public class ControllerCacheLoader implements CacheLoader
{
   /**
    * The nested cache loader
    */
   private final CacheLoader cl;

   /**
    * The related cache
    */
   private CacheSPI cache;

   /**
    * The configuration of the current cache loader
    */
   private IndividualCacheLoaderConfig config;

   /**
    * The default constructor
    * @param cl the cache loader that will be managed by the controller
    */
   public ControllerCacheLoader(CacheLoader cl)
   {
      this.cl = cl;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#commit(java.lang.Object)
    */
   public void commit(final Object tx) throws Exception
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            cl.commit(tx);
            return null;
         }
      };
      try
      {
         AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();

         if (cause instanceof Exception)
         {
            throw (Exception)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#exists(org.jboss.cache.Fqn)
    */
   public boolean exists(Fqn name) throws Exception
   {
      if (cache.getCacheStatus() == CacheStatus.STARTING)
      {
         // Before calling the nested cache loader we first check if the data exists in the local cache
         // in order to prevent multiple call to the cache store         
         NodeSPI<?, ?> node = cache.peek(name, false);
         if (node != null)
         {
            // The node already exists in the local cache, so we return true
            return true;
         }
         else
         {
            // The node doesn't exist in the local cache, so we need to check through the nested
            // cache loader
            return cl.exists(name);
         }
      }
      // All the data is loaded at startup, so no need to call the nested cache loader for another
      // cache status other than CacheStatus.STARTING
      return false;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#get(org.jboss.cache.Fqn)
    */
   public Map<Object, Object> get(Fqn name) throws Exception
   {
      if (cache.getCacheStatus() == CacheStatus.STARTING)
      {
         // Before calling the nested cache loader we first check if the data exists in the local cache
         // in order to prevent multiple call to the cache store                  
         NodeSPI node = cache.peek(name, false);
         if (node != null)
         {
            // The node already exists in the local cache, so we return the corresponding data
            return node.getDataDirect();
         }
         else
         {
            // The node doesn't exist in the local cache, so we need to check through the nested
            // cache loader            
            return cl.get(name);
         }
      }
      // All the data is loaded at startup, so no need to call the nested cache loader for another
      // cache status other than CacheStatus.STARTING
      return null;
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#getChildrenNames(org.jboss.cache.Fqn)
    */
   public Set<?> getChildrenNames(Fqn fqn) throws Exception
   {
      return cl.getChildrenNames(fqn);
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
      cl.loadEntireState(os);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#loadState(org.jboss.cache.Fqn, java.io.ObjectOutputStream)
    */
   public void loadState(Fqn subtree, ObjectOutputStream os) throws Exception
   {
      cl.loadState(subtree, os);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#prepare(java.lang.Object, java.util.List, boolean)
    */
   public void prepare(final Object tx, final List<Modification> modifications, final boolean onePhase)
      throws Exception
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            cl.prepare(tx, modifications, onePhase);
            return null;
         }
      };
      try
      {
         AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();

         if (cause instanceof Exception)
         {
            throw (Exception)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(java.util.List)
    */
   public void put(List<Modification> modifications) throws Exception
   {
      cl.put(modifications);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.util.Map)
    */
   public void put(Fqn name, Map<Object, Object> attributes) throws Exception
   {
      cl.put(name, attributes);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(final Fqn name, final Object key, final Object value) throws Exception
   {
      return SecurityHelper.doPriviledgedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            return cl.put(name, key, value);
         }
      });
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#remove(org.jboss.cache.Fqn)
    */
   public void remove(Fqn fqn) throws Exception
   {
      cl.remove(fqn);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#remove(org.jboss.cache.Fqn, java.lang.Object)
    */
   public Object remove(Fqn fqn, Object key) throws Exception
   {
      return cl.remove(fqn, key);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#removeData(org.jboss.cache.Fqn)
    */
   public void removeData(Fqn fqn) throws Exception
   {
      cl.removeData(fqn);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#rollback(java.lang.Object)
    */
   public void rollback(final Object tx)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            cl.rollback(tx);
            return null;
         }
      };
      AccessController.doPrivileged(action);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#setCache(org.jboss.cache.CacheSPI)
    */
   public void setCache(CacheSPI c)
   {
      cl.setCache(c);
      this.cache = c;
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
      cl.setRegionManager(manager);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#storeEntireState(java.io.ObjectInputStream)
    */
   public void storeEntireState(ObjectInputStream is) throws Exception
   {
      cl.storeEntireState(is);
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#storeState(org.jboss.cache.Fqn, java.io.ObjectInputStream)
    */
   public void storeState(Fqn subtree, ObjectInputStream is) throws Exception
   {
      cl.storeState(subtree, is);
   }

   /**
    * @see org.jboss.cache.Lifecycle#create()
    */
   public void create() throws Exception
   {
      cl.create();
   }

   /**
    * @see org.jboss.cache.Lifecycle#destroy()
    */
   public void destroy()
   {
      cl.destroy();
   }

   /**
    * @see org.jboss.cache.Lifecycle#start()
    */
   public void start() throws Exception
   {
      cl.start();
   }

   /**
    * @see org.jboss.cache.Lifecycle#stop()
    */
   public void stop()
   {
      cl.stop();
   }

}
