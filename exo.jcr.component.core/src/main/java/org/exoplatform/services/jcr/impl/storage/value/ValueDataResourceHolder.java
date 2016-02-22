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
package org.exoplatform.services.jcr.impl.storage.value;

import org.exoplatform.services.jcr.impl.storage.value.fs.operations.ValueLockSupport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * 
 * This is singleton object for resources holding.
 * 
 * <br>
 * Date: 01.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueDataResourceHolder.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ValueDataResourceHolder
{

   /**
    * Resources map.
    */
   protected final Map<Object, VDResource> resources = new ConcurrentHashMap<Object, VDResource>();

   /**
    * ValueData resource holder.
    *
    */
   class VDResource
   {
      /**
       * User Thread.
       */
      final Thread user;

      /**
       * Per-JVM lock.
       */
      final Object lock;

      /**
       * Lock support.
       */
      final ValueLockSupport lockSupport;

      /**
       * Same user locks count.
       */
      int userLocks = 0;

      /**
       * VDResource  constructor.
       *
       * @param user Thread
       * @param lock Object
       * @param lockSupport ValueLockSupport
       * @throws IOException if lock error occurs
       */
      VDResource(Thread user, Object lock, ValueLockSupport lockSupport) throws IOException
      {
         this.user = user;
         this.lock = lock;
         this.lockSupport = lockSupport;

         lockSupport.lock();

         userLocks = 1;
      }

      /**
       * Add user lock.
       * 
       * Lock can be added by only the lock creator user (Thread recorded in user variable).
       * 
       * @param user
       *          Thread
       * @param lockSupport
       *          ValueLockSupport
       * @return boolean, true if the resource reaquired by the same user
       * @throws IOException
       *           if share is not supported
       */
      boolean addUserLock(Thread user, ValueLockSupport lockSupport) throws IOException
      {
         if (this.user.equals(user))
         {
            lockSupport.share(this.lockSupport);
            userLocks++;
            return true;
         }

         return false;
      }

      /**
       * Remove user lock.
       * 
       * Lock can be removed by only the lock creator user (Thread recorded in user variable).
       * 
       * @param user
       *          Thread
       * @return boolean true - if no more locks of this user on the resource, false - otherwise
       */
      boolean removeUserLock(Thread user)
      {
         if (this.user.equals(user))
         {
            userLocks--;
            return userLocks <= 0;
         }

         return false;
      }
   }

   /**
    * Aquire ValueData resource.
    * 
    * @param resource
    *          Object
    * @param lockHolder
    *          ValueLockSupport
    * @throws InterruptedException
    *           if resource lock is interrupted
    * @return boolean, false - if the resource reaquired by the same user (Thread), true otherwise
    * @throws IOException
    *           if lock error occurs
    */
   public boolean aquire(final Object resource, final ValueLockSupport lockHolder) throws InterruptedException,
      IOException
   {
      final Thread myThread = Thread.currentThread();
      final VDResource res = resources.get(resource);
      if (res != null)
      {
         if (res.addUserLock(myThread, lockHolder))
            // resource locked in this thread (by me)
            return false;

         synchronized (res.lock)
         {
            // resource locked, wait for unlock
            res.lock.wait();
            // new record with existing lock (to respect Object.notify())
            resources.put(resource, new VDResource(myThread, res.lock, lockHolder));
         }
      }
      else
         resources.put(resource, new VDResource(myThread, new Object(), lockHolder));

      return true;
   }

   /**
    * Release resource.
    * 
    * @param resource
    *          Object
    * @return boolean, true - if resource is released, false - if the resource still in use.
    * @throws IOException
    *           if unlock error occurs
    */
   public boolean release(final Object resource) throws IOException
   {
      final Thread myThread = Thread.currentThread();
      final VDResource res = resources.get(resource);
      if (res != null)
      {
         if (res.removeUserLock(myThread))
         {
            synchronized (res.lock)
            {
               // unlock holder
               res.lockSupport.unlock();

               // locked by this thread (by me)
               // ...Wakes up a single thread that is waiting on this object's monitor
               res.lock.notify();
               // resources will be reputed with new VDResource in aquire() of another Thread
               resources.remove(resource);
            }
            return true;
         }
      }

      return false;
   }
}
