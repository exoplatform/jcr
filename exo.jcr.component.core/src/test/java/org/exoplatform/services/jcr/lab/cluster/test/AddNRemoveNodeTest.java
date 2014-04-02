/*
 * Copyright (C) 2014 eXo Platform SAS.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class AddNRemoveNodeTest extends JcrAPIBaseTest
{
   private final int MAX_ITERATIONS = 20000;
   private final int TOTAL_THREAD = 20;
   private Node rootTestNode;
   private AtomicInteger count = new AtomicInteger();

   public void setUp() throws Exception
   {
      super.setUp();

      if (root.hasNode("AddNRemoveNodeTest"))
      {
         rootTestNode = root.getNode("AddNRemoveNodeTest");
      }
      else
      {
         rootTestNode = root.addNode("AddNRemoveNodeTest");
         root.save();
      }
   }

   protected void tearDown()
   {
      // Don't remove the node as it could affect other cluster nodes still running
   }

   private void addNRemoveNode(AtomicBoolean stop, Session session) throws Exception
   {
      int iteration = count.incrementAndGet();
      if (iteration >= MAX_ITERATIONS)
      {
         stop.compareAndSet(false, true);
         return;
      }
      String name = System.currentTimeMillis() + "-" + iteration;
      try
      {
         Node n = ((Node)session.getItem(rootTestNode.getPath())).addNode(name);
         session.save();
         log.info("Node '{}' has been added", name);
         n.remove();
         session.save();
         log.info("Node '{}' has been removed", name);
      }
      catch (Exception e)
      {
         log.error("Could not add/remove the node '{}' due to the error: {}", name, e.getMessage());
         log.error("error", e);
      }
   }

   public void testAddNRemoveNode() throws Exception
   {
      final AtomicBoolean stop = new AtomicBoolean();
      final CountDownLatch startSignal = new CountDownLatch(1);
      final CountDownLatch endSignal = new CountDownLatch(TOTAL_THREAD);
      Runnable task = new Runnable()
      {
         public void run()
         {
            Session session = null;
            try
            {
               log.info("Ready to start");
               startSignal.await();
               log.info("Started");
               session = repository.login(credentials, "ws");
               while (!stop.get())
               {
                  addNRemoveNode(stop, session);
                  Thread.sleep(20);
               }
               log.info("Stopped");
            }
            catch (Exception e)
            {
               log.info("Failed", e);
            }
            finally
            {
               if (session != null)
                  session.logout();
               endSignal.countDown();
            }
         }
      };
      for (int i = 1; i <= TOTAL_THREAD; i++)
      {
         new Thread(task, "AddNRemoveNodeTest-" + i).start();
      }
      log.info("Launching the threads");
      startSignal.countDown();
      log.info("Waiting until we reach {} iterations");
      endSignal.await();
   }
}
