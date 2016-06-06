/*
 * Copyright (C) 2016 eXo Platform SAS.
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
package org.exoplatform.services.jcr.api.core.query.lucene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.exoplatform.services.jcr.api.core.query.AbstractIndexingTest;

/**
 * @author aboughzela@exoplatform.com
 */
public class ConcurrentQueriesWithUpdatesTest extends AbstractIndexingTest
{private static final int NUM_UPDATES = 50;

   private int numNodes;

   @Override
   protected void setUp() throws Exception {
      super.setUp();
      numNodes = createNodes(testRootNode, 2, 12, 0);
      superuser.save();
   }

   public void testQueriesWithUpdates() throws Exception {
      final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
      final AtomicBoolean running = new AtomicBoolean(true);
      // track executed queries and do updates
      final BlockingQueue<Object> queryExecuted = new LinkedBlockingQueue<Object>();
      Thread queries = new Thread(new Runnable() {
         public void run() {
            try {
               runTask(new Task() {
                  public void execute(Session session, Node test)
                     throws RepositoryException {
                     QueryManager qm = session.getWorkspace().getQueryManager();
                     while (running.get()) {
                        Query q = qm.createQuery(testPath + "//element(*, nt:unstructured) order by @jcr:score descending", Query.XPATH);
                        NodeIterator nodes = q.execute().getNodes();
                        assertEquals("wrong result set size", numNodes, nodes.getSize());
                        queryExecuted.offer(new Object());
                     }
                  }
               }, 5, testRootNode.getPath());
            } catch (RepositoryException e) {
               exceptions.add(e);
            }
         }
      });
      queries.start();
      Thread update = new Thread(new Runnable() {
         public void run() {
            try {
               runTask(new Task() {
                  public void execute(Session session, Node test)
                     throws RepositoryException {
                     Random rand = new Random();
                     QueryManager qm = session.getWorkspace().getQueryManager();
                     for (int i = 0; i < NUM_UPDATES; i++) {
                        try {
                           // wait at most 10 seconds
                           queryExecuted.poll(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                           // ignore
                        }
                        Query q = qm.createQuery(testPath + "//node" + rand.nextInt(numNodes) + " order by @jcr:score descending", Query.XPATH);
                        NodeIterator nodes = q.execute().getNodes();
                        if (nodes.hasNext()) {
                           Node n = nodes.nextNode();
                           n.setProperty("foo", "bar");
                           session.save();
                        }
                     }
                  }
               }, 1, testRootNode.getPath());
            } catch (RepositoryException e) {
               exceptions.add(e);
            }
         }
      });
      update.start();
      update.join();
      running.set(false);
      queries.join();
   }

   private int createNodes(Node n, int nodesPerLevel, int levels, int count)
      throws RepositoryException {
      levels--;
      for (int i = 0; i < nodesPerLevel; i++) {
         Node child = n.addNode("node" + count);
         count++;
         if (count % 1000 == 0) {
            superuser.save();
         }
         if (levels > 0) {
            count = createNodes(child, nodesPerLevel, levels, count);
         }
      }
      return count;
   }

   /**
    * Runs a task with the given concurrency on the node identified by path.
    *
    * @param task the task to run.
    * @param concurrency the concurrency.
    * @param path the path to the test node.
    * @throws RepositoryException if an error occurs.
    */
   protected void runTask(Task task, int concurrency, String path)
      throws RepositoryException {
      Executor[] executors = new Executor[concurrency];
      for (int i = 0; i < concurrency; i++) {
         Session s = testRootNode.getSession();
         Node test = (Node) s.getItem(path);
         s.save();
         executors[i] = new Executor(s, test, task);
      }
      executeAll(executors);
   }

   /**
    * Task implementations must be thread safe! Multiple threads will call
    * {@link #execute(Session, Node)} concurrently.
    */
   public interface Task {

      public abstract void execute(Session session, Node test)
         throws RepositoryException;
   }

   protected static class Executor implements Runnable {

      protected final Session session;

      protected final Node test;

      protected final Task task;

      protected RepositoryException exception;

      public Executor(Session session, Node test, Task task) {
         this.session = session;
         this.test = test;
         this.task = task;
      }

      public RepositoryException getException() {
         return exception;
      }

      public void run() {
         try {
            task.execute(session, test);
         } catch (RepositoryException e) {
            exception = e;
         } finally {
            session.logout();
         }
      }
   }

   /**
    * Executes all executors using individual threads.
    *
    * @param executors the executors.
    * @throws RepositoryException if one of the executors throws an exception.
    */
   private void executeAll(Executor[] executors) throws RepositoryException
   {
      Thread[] threads = new Thread[executors.length];
      for (int i = 0; i < executors.length; i++)
      {
         threads[i] = new Thread(executors[i], "Executor " + i);
      }
      for (int i = 0; i < threads.length; i++)
      {
         threads[i].start();
      }
   }
}
