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
package org.exoplatform.services.jcr.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: PrivilegedCacheHelper.java 111 2010-11-11 11:11:11Z tolusha $
 *
 */
public class PrivilegedJBossCacheHelper
{
   /**
    * Start JBoss cache in privileged mode.
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
    * Stop JBoss cache in privileged mode.
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
    * Create JBoss cache in privileged mode.
    * 
    * @param cache
    */
   public static void create(final Cache<Serializable, Object> cache)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            cache.create();
            return null;
         }
      };
      SecurityHelper.doPrivilegedAction(action);
   }

   /**
    * Put in JBoss cache in privileged mode.
    * 
    * @param cache
    */
   public static Object put(final Cache<Serializable, Object> cache, final Fqn fqn, final Serializable key,
      final Object value) throws CacheException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            return cache.put(fqn, key, value);
         }
      };
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof CacheException)
         {
            throw (CacheException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Revemo node in JBoss cache in privileged mode.
    * 
    * @param cache
    */
   public static boolean removeNode(final Cache<Serializable, Object> cache, final Fqn fqn) throws CacheException
   {
      PrivilegedExceptionAction<Boolean> action = new PrivilegedExceptionAction<Boolean>()
      {
         public Boolean run() throws Exception
         {
            return cache.removeNode(fqn);
         }
      };
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof CacheException)
         {
            throw (CacheException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }
}
