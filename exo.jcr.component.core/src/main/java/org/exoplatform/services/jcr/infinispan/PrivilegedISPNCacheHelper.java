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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.infinispan.Cache;

import java.io.Serializable;
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
      SecurityHelper.doPrivilegedAction(action);
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
      SecurityHelper.doPrivilegedAction(action);
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
      return SecurityHelper.doPrivilegedAction(action);
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
      return SecurityHelper.doPrivilegedAction(action);
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
      return SecurityHelper.doPrivilegedAction(action);
   }
}
