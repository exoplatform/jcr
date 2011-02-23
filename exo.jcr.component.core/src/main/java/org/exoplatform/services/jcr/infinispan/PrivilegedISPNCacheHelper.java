/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import org.infinispan.Cache;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: PrivilegedCacheHelper.java 111 2010-11-11 11:11:11Z tolusha $
 *
 */
public class PrivilegedISPNCacheHelper
{

   /**
    * Start Infinispan cache in privileged mode.
    * 
    * @param cache
    */
   public static void start(final Cache<Serializable, Object> cache)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            cache.start();
            return null;
         }
      };
      AccessController.doPrivileged(action);
   }

   /**
    * Stop Infinispan cache in privileged mode.
    * 
    * @param cache
    */
   public static void stop(final Cache<Serializable, Object> cache)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            cache.stop();
            return null;
         }
      };
      AccessController.doPrivileged(action);
   }

   /**
    * Put in Infinispan cache in privileged mode.
    * 
    * @param cache
    */
   public static Object putIfAbsent(final Cache<Serializable, Object> cache, final Serializable key, final Object value)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return cache.putIfAbsent(key, value);
         }
      };
      return AccessController.doPrivileged(action);
   }

   /**
    * Put in Infinispan cache in privileged mode.
    * 
    * @param cache
    */
   public static Object put(final Cache<Serializable, Object> cache, final Serializable key, final Object value)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return cache.put(key, value);
         }
      };
      return AccessController.doPrivileged(action);
   }

   /**
    * Put in Infinispan cache in privileged mode.
    * 
    * @param cache
    */
   public static Object put(final Cache<Serializable, Object> cache, final Serializable key, final Object value,
      final long lifespan, final TimeUnit unit)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return cache.put(key, value, lifespan, unit);
         }
      };
      return AccessController.doPrivileged(action);
   }
}
