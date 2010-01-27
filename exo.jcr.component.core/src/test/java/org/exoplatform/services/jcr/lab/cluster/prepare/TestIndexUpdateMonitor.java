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
package org.exoplatform.services.jcr.lab.cluster.prepare;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.jbosscache.JBossCacheIndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.lock.LockType;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class TestIndexUpdateMonitor extends TestCase
{
   /**
    * Logger instance for this class
    */
   private final Log log = ExoLogger.getLogger(TestIndexUpdateMonitor.class);

   private IndexUpdateMonitor indexUpdateMonitor;

   private Cache<Serializable, Object> cache;

   /**
    * @see org.exoplatform.services.jcr.BaseStandaloneTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      // TODO Auto-generated method stub
      super.setUp();
      cache = createCache();
      indexUpdateMonitor = new JBossCacheIndexUpdateMonitor(cache, new IndexerIoModeHandler(IndexerIoMode.READ_WRITE));
   }

   /**
    * @see junit.framework.TestCase#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {
      // TODO Auto-generated method stub
      super.tearDown();
      cache.destroy();
   }

   public void testSimpleBoolean() throws Exception
   {
      //test default
      assertFalse(indexUpdateMonitor.getUpdateInProgress());

      //test set false
      indexUpdateMonitor.setUpdateInProgress(false);
      assertFalse(indexUpdateMonitor.getUpdateInProgress());

      //test set true
      indexUpdateMonitor.setUpdateInProgress(true);
      assertTrue(indexUpdateMonitor.getUpdateInProgress());

      //test set false
      indexUpdateMonitor.setUpdateInProgress(false);
      assertFalse(indexUpdateMonitor.getUpdateInProgress());

   }

   public void testLock() throws Exception
   {
      String lockName = "testLock";
      assertFalse(indexUpdateMonitor.isLocked(lockName));
      assertTrue(indexUpdateMonitor.lock(lockName, LockType.WRITE));
      assertTrue(indexUpdateMonitor.isLocked(lockName));
      LockChecker checker = new LockChecker(indexUpdateMonitor, lockName);
      Thread lockThread = new Thread(checker);
      assertFalse(checker.isWaiting());
      lockThread.start();
      assertTrue(checker.isWaiting());
      indexUpdateMonitor.unlock(lockName);
      assertFalse(checker.isWaiting());

   }

   public void _testMultiThread() throws Exception
   {
      AtomicBoolean atomicBoolean = new AtomicBoolean();
      ThreadGroup chengers = new ThreadGroup("Changers");
      ThreadGroup checkers = new ThreadGroup("Checkers");
      Thread[] changersArray = new Thread[10];
      Thread[] checkerArray = new Thread[10];

      for (int i = 0; i < changersArray.length; i++)
      {
         changersArray[i] = new Thread(chengers, new UpdateMonitorChanger(atomicBoolean));
         changersArray[i].start();
      }

      for (int i = 0; i < checkerArray.length; i++)
      {
         checkerArray[i] = new Thread(checkers, new UpdateMonitorChecker(atomicBoolean));
         checkerArray[i].start();
      }

      //      Thread changer = new Thread(new UpdateMonitorChanger(atomicBoolean));
      //      changer.start();
      //      Thread checker = new Thread(new UpdateMonitorChecker(atomicBoolean));
      //      checker.start();

      Thread.sleep(4 * 60 * 1000);
      chengers.destroy();
      checkers.destroy();
   }

   private class LockChecker implements Runnable
   {
      private final String lockName;

      private boolean waiting = false;

      /**
       * @return the waiting
       */
      protected boolean isWaiting()
      {
         return waiting;
      }

      /**
       * @param indexUpdateMonitor
       */
      public LockChecker(IndexUpdateMonitor indexUpdateMonitor, String lockName)
      {
         super();
         this.indexUpdateMonitor = indexUpdateMonitor;
         this.lockName = lockName;
      }

      private final IndexUpdateMonitor indexUpdateMonitor;

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         waiting = true;
         assertTrue(indexUpdateMonitor.lock(lockName, LockType.WRITE));
         waiting = false;
      }
   }

   private class UpdateMonitorChanger implements Runnable
   {
      private AtomicBoolean atomicBoolean;

      /**
       * @param atomicBoolean
       */
      public UpdateMonitorChanger(AtomicBoolean atomicBoolean)
      {
         super();
         this.atomicBoolean = atomicBoolean;
      }

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         while (!Thread.currentThread().isInterrupted())
         {

            synchronized (atomicBoolean)
            {
               assertEquals(atomicBoolean.get(), indexUpdateMonitor.getUpdateInProgress());
               boolean oldValue = atomicBoolean.get();

               indexUpdateMonitor.setUpdateInProgress(!oldValue);

               if (!atomicBoolean.compareAndSet(oldValue, !oldValue))
               {
                  log.warn("Fail to change monitor");
               }
            }
         }

      }
   }

   private class UpdateMonitorChecker implements Runnable
   {

      /**
       * @param atomicBoolean
       */
      public UpdateMonitorChecker(AtomicBoolean atomicBoolean)
      {
         super();
         this.atomicBoolean = atomicBoolean;
      }

      private final AtomicBoolean atomicBoolean;

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {

         while (!Thread.currentThread().isInterrupted())
         {

            synchronized (atomicBoolean)
            {
               //assertEquals(atomicBoolean.get(), indexUpdateMonitor.getUpdateInProgress());
               if (atomicBoolean.get() == indexUpdateMonitor.getUpdateInProgress())
               {
                  System.out.println("check ok");
               }
               else
               {
                  System.out.println("check fail");
               }

            }

            try
            {
               Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
               return;
            }
         }

      }
   }

   private Cache<Serializable, Object> createCache()
   {
      String jbcConfig = "conf/cluster/test-jbosscache-indexer-config-exoloader_db1_ws.xml";

      CacheFactory<Serializable, Object> factory = new DefaultCacheFactory<Serializable, Object>();
      log.info("JBoss Cache configuration used: " + jbcConfig);
      Cache<Serializable, Object> cache = factory.createCache(jbcConfig, false);

      cache.create();
      cache.start();

      return cache;
   }

}
