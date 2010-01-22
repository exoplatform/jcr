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
package org.exoplatform.services.jcr.lab.cache;

import junit.framework.TestCase;

import org.jboss.cache.CacheFactory;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.DefaultCacheFactory;

import java.io.Serializable;
import java.util.concurrent.CyclicBarrier;

import javax.transaction.TransactionManager;

/**
 * This test created two caches with replication between them. Then two different thread
 * are writing to the same FQN some values. Then they commit transactions (in the same 
 * time).
 * Wait for 20s and you'll get:
 * org.jboss.cache.lock.TimeoutException: Unable to acquire lock on Fqn [/fqn] after [20000] milliseconds for requestor 
 *          [GlobalTransaction:<127.0.0.1:9601>:1]! Lock held by [GlobalTransaction:<127.0.0.1:9600>:0]
 *          
 * org.jboss.cache.lock.TimeoutException: Unable to acquire lock on Fqn [/fqn] after [20000] milliseconds for requestor 
 *          [GlobalTransaction:<127.0.0.1:9600>:0]! Lock held by [GlobalTransaction:<127.0.0.1:9601>:1]
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: Test.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestCacheReplicationDeadLock extends TestCase
{

   int threadCount = 2;

   private final CyclicBarrier barrier = new CyclicBarrier(threadCount);

   private final static String LONG_TEXT =
      "Compared to OHV pushrod (or I-Head) "
         + "systems with the same number of valves the reciprocating components of "
         + "the OHC system are fewer and have a lower total mass. Though the system"
         + " that drives the cams may become more complex, most engine manufacturer"
         + "s easily accept that added complexity in trade for better engine perfor"
         + "mance and greater design flexibility. Another performance advantage is "
         + "gained as a result of the better optimized port configurations made pos"
         + "sible with overhead camshaft designs. With no intrusive pushrods the ov"
         + "erhead camshaft cylinder head design can use straighter ports of more a"
         + "dvantageous crossection and length.";

   public void testDeadLock()
   {
      // create cache
      CacheFactory<Serializable, Object> factory = new DefaultCacheFactory<Serializable, Object>();
      final CacheSPI<Serializable, Object> cache1 =
         (CacheSPI<Serializable, Object>)factory.createCache("conf/cluster/test-replication-deadlock.xml", false);
      final CacheSPI<Serializable, Object> cache2 =
         (CacheSPI<Serializable, Object>)factory.createCache("conf/cluster/test-replication-deadlock.xml", false);

      cache1.create();
      cache2.create();

      cache1.start();
      cache2.start();
      // create threads
      for (int i = 0; i < threadCount; i++)
      {
         new Thread(new WritingTask(barrier, i, i % 2 == 0 ? cache1 : cache2)).start();
      }

      // wait a minute and tear down
      try
      {
         Thread.sleep(60000);
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }

      cache1.stop();
      cache1.destroy();
      cache2.stop();
      cache2.destroy();
   }

   class WritingTask implements Runnable
   {

      private CyclicBarrier barrier;

      private int index;

      private CacheSPI<Serializable, Object> cache;

      public WritingTask(CyclicBarrier barrier, int index, CacheSPI<Serializable, Object> cache)
      {
         super();
         this.barrier = barrier;
         this.index = index;
         this.cache = cache;
      }

      public void run()
      {
         try
         {
            while (true)
            {
               TransactionManager mgr = cache.getTransactionManager();
               mgr.begin();
               cache.put("fqn", "key", LONG_TEXT + index);
               // make them save in one moment
               barrier.await();
               mgr.commit();
               System.out.print(index + " ");
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

   }

}
