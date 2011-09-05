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
package org.exoplatform.services.jcr.lab.cluster.test;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.jboss.cache.lock.TimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Test that proves BufferedJBossCache as a solution against dead-locks.
 * Was tested with lockAcquisitionTimeout="10000" (10s).
 * If ordering is not done in BufferedJBossCache then this test shown TimeOutException
 * when one thread waits for another one. Like this:
 * 
 * org.jboss.cache.lock.TimeoutException: Unable to acquire lock on Fqn [/$CHILD_NODES_LIST/2235b1da7f0000014f62cf6a381e342f]
 *     after [10000] milliseconds for requestor [GlobalTransaction:<null>:243]! Lock held by [GlobalTransaction:<null>:244]
 * org.jboss.cache.lock.TimeoutException: Unable to acquire lock on Fqn [/$CHILD_NODES_LIST/2235b1d67f0000017209d9af66cfae9a] 
 *     after [10000] milliseconds for requestor [GlobalTransaction:<null>:244]! Lock held by [GlobalTransaction:<null>:243]
 *     
 * When running this test you should manually check that this is really a dead-lock when the first thread is waiting for 
 * the second and the second for the first one. (like example when TX243 waits TX244 and contrary)    
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestLoadIndexerWriter.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestCacheDeadLockDetection extends JcrAPIBaseTest
{
   public static final String NODEANAME = "nodeA";

   public static final String NODEBNAME = "nodeB";

   public static final String LONGTEXT =
      "A double overhead camshaft valve train layout is characterized by two camshafts"
         + " located within the cylinder head, one operating the inlet valves and one operating the exhaust valves. Some en"
         + "gines have more than one bank of cylinder heads (V8 and flat-four being two well-known examples) and these have"
         + " more than two camshafts in total, but they remain DOHC. The term 'twin cam' is imprecise, but will normally re"
         + "fer to a DOHC engine. Some manufacturers still managed to use a SOHC in 4-valve layouts. Honda for instan"
         + "ce with the later half of the D16 family, this is usually done to reduce overall costs. Also not all DOHC"
         + " engines are multivalve engines—DOHC was common in two valve per cylinder heads for decades before multiv"
         + "alve heads appeared. Today, however, DOHC is synonymous with multi-valve heads since almost all DOHC engi"
         + "nes have between three and five valves per cylinder.";

   private boolean stop = false;

   private int threadCount = 2;

   private int propertyCount = 200;

   private int nodeCount = 10;

   private CountDownLatch latch;

   public void testWrite() throws RepositoryException
   {
      log.info("Creating threads...");
      root.addNode(NODEANAME);
      root.addNode(NODEBNAME);
      session.save();
      //
      List<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < threadCount; i++)
      {
         // create new thread and start it
         threads.add(new Thread(new WriterTask(i)));
      }
      for (Thread thread : threads)
      {
         thread.start();
      }
      log.info("Threads created and started.");
      // wait 4 minutes
      sleep(60000 * 4);

      stop = true;
   }

   /**
    * sleeps for milliseconds.
    * 
    * @param time
    */
   private void sleep(long time)
   {
      try
      {
         Thread.sleep(time);
      }
      catch (InterruptedException e)
      {
         log.error(e);
      }
   }

   /**
    * returns "active" latch if it is not null and not achieved 0.
    * otherwise returns new latch.
    * @return
    */
   private synchronized CountDownLatch getLatch()
   {
      if (latch == null || latch.getCount() == 0)
      {
         latch = new CountDownLatch(threadCount);
      }
      return latch;
   }

   /**
    * This is the runnable task, that performs write to 2 different nodes, but in different contrary order. 
    */
   private class WriterTask implements Runnable
   {
      private int id;

      private SessionImpl sessionLocal;

      private Node nodeA;

      private Node nodeB;

      public WriterTask(int id) throws RepositoryException
      {
         this.id = id;
         // login
         CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
         sessionLocal = (SessionImpl)repository.login(credentials, "ws");
         // prepare nodes
         Node root = sessionLocal.getRootNode();
         nodeA = root.getNode(NODEANAME);
         nodeB = root.getNode(NODEBNAME);
         sessionLocal.save();
      }

      public void run()
      {
         try
         {
            int index = 0;
            while (!stop)
            {
               index++;
               if (id % 2 == 0)
               {
                  fillNode(nodeA, index);
                  fillNode(nodeB, index);
               }
               else
               {
                  fillNode(nodeB, index);
                  fillNode(nodeA, index);
               }
               // session.save() should be synchronized between threads.
               CountDownLatch latch = getLatch();
               latch.countDown();
               try
               {
                  // when all threads are ready, await will resume the control flow
                  latch.await();
               }
               catch (InterruptedException e)
               {
               }
               sessionLocal.save();
               System.out.print("#");
            }
         }
         catch (RepositoryException e)
         {
            log.error(e);
         }
         catch (TimeoutException e)
         {
            log.error(e);
         }
      }

      /**
       * Simply adds lots of nodes and properties to the given parent.
       * 
       * @param node
       * @param index
       * @throws RepositoryException
       */
      public void fillNode(Node node, int index) throws RepositoryException
      {
         for (int n = 0; n < nodeCount; n++)
         {
            Node child = node.addNode("child_" + n + "_" + id + index);
            for (int p = 0; p < propertyCount; p++)
            {
               child.setProperty("string_" + p + "_" + id + index, LONGTEXT);
            }
         }
         node.setProperty("string_" + id + index, LONGTEXT);
      }
   }

}
