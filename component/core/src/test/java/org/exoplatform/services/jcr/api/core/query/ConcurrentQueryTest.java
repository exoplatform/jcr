/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.api.core.query;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.log.Log;
import org.apache.jackrabbit.test.AbstractJCRTest;

import org.exoplatform.services.log.ExoLogger;

/**
 * Runs queries in one thread while another thread is modifying the workspace.
 */
public class ConcurrentQueryTest
   extends AbstractJCRTest
{

   /**
    * Number of threads executing queries.
    */
   private static final int NUM_READERS = 2;

   /**
    * The read sessions executing the queries.
    */
   private List<Session> readSessions = new ArrayList<Session>();

   /**
    * Gets the read sessions for the test cases.
    */
   protected void setUp() throws Exception
   {
      super.setUp();
      for (int i = 0; i < NUM_READERS; i++)
      {
         readSessions.add(helper.getReadOnlySession());
      }
   }

   /**
    * Logs out the sessions aquired in setUp().
    */
   protected void tearDown() throws Exception
   {

      super.tearDown();
      for (Iterator<Session> it = readSessions.iterator(); it.hasNext();)
      {
         it.next().logout();
      }
      readSessions.clear();
   }

   /**
    * Writes 1000 nodes in transactions of 5 nodes to the workspace while other threads query the
    * workspace. Query results must always return a consistent view of the workspace, that is:<br/>
    * <code>result.numNodes % 5 == 0</code>
    */
   public void testConcurrentQueryWithWrite() throws Exception
   {

      final List<RepositoryException> exceptions = Collections.synchronizedList(new ArrayList<RepositoryException>());
      List<QueryWorker> readers = new ArrayList<QueryWorker>();
      String query = "/jcr:root" + testRoot + "//*[@testprop = 'foo']";
      for (Iterator<Session> it = readSessions.iterator(); it.hasNext();)
      {
         Session s = it.next();
         readers.add(new QueryWorker(s, query, exceptions, log));
      }

      Thread writer = new Thread()
      {
         public void run()
         {
            try
            {
               for (int i = 0; i < 20; i++)
               {
                  Node n = testRootNode.addNode("node" + i);
                  for (int j = 0; j < 10; j++)
                  {
                     Node n1 = n.addNode("node" + j);
                     for (int k = 0; k < 5; k++)
                     {
                        n1.addNode("node" + k).setProperty("testprop", "foo");
                     }
                     testRootNode.save();
                  }
               }
            }
            catch (RepositoryException e)
            {
               exceptions.add(e);
            }
         }
      };

      // start the threads
      writer.start();
      for (Iterator<QueryWorker> it = readers.iterator(); it.hasNext();)
      {
         it.next().start();
      }

      // wait for writer thread to finish its work
      writer.join();

      // request readers to finish
      for (Iterator<QueryWorker> it = readers.iterator(); it.hasNext();)
      {
         QueryWorker t = it.next();
         t.finish();
         t.join();
      }

      // fail in case of exceptions
      if (exceptions.size() > 0)
      {
         fail(exceptions.get(0).toString());
      }
   }

   /**
    * Deletes 1000 nodes in transactions of 5 nodes while other threads query the workspace. Query
    * results must always return a consistent view of the workspace, that is:<br/>
    * <code>result.numNodes % 5 == 0</code>
    */
   public void testConcurrentQueryWithDeletes() throws Exception
   {

      // create 1000 nodes
      for (int i = 0; i < 20; i++)
      {
         Node n = testRootNode.addNode("node" + i);
         for (int j = 0; j < 10; j++)
         {
            Node n1 = n.addNode("node" + j);
            for (int k = 0; k < 5; k++)
            {
               n1.addNode("node" + k).setProperty("testprop", "foo");
            }
         }
         testRootNode.save();
      }

      final List<RepositoryException> exceptions = Collections.synchronizedList(new ArrayList<RepositoryException>());
      List<QueryWorker> readers = new ArrayList<QueryWorker>();
      String query = "/jcr:root" + testRoot + "//*[@testprop = 'foo']";
      for (Iterator<Session> it = readSessions.iterator(); it.hasNext();)
      {
         Session s = it.next();
         readers.add(new QueryWorker(s, query, exceptions, log));
      }

      Thread writer = new Thread()
      {
         public void run()
         {
            try
            {
               for (int i = 0; i < 20; i++)
               {
                  Node n = testRootNode.getNode("node" + i);
                  for (int j = 0; j < 10; j++)
                  {
                     Node n1 = n.getNode("node" + j);
                     for (int k = 0; k < 5; k++)
                     {
                        n1.getNode("node" + k).remove();
                     }
                     testRootNode.save();
                  }
               }
            }
            catch (RepositoryException e)
            {
               exceptions.add(e);
            }
         }
      };

      // start the threads
      writer.start();
      for (Iterator<QueryWorker> it = readers.iterator(); it.hasNext();)
      {
         it.next().start();
      }

      // wait for writer thread to finish its work
      writer.join();

      // request readers to finish
      for (Iterator<QueryWorker> it = readers.iterator(); it.hasNext();)
      {
         QueryWorker t = it.next();
         t.finish();
         t.join();
      }

      // fail in case of exceptions
      if (exceptions.size() > 0)
      {
         exceptions.get(0).printStackTrace();
         fail(exceptions.get(0).toString());
      }
   }

   /**
    * Executes queries in a separate thread.
    */
   private static final class QueryWorker
      extends Thread
   {

      private Session s;

      private String query;

      private final List<RepositoryException> exceptions;

      // private final PrintWriter log;
      private boolean finish = false;

      private int count;

      /**
       * The logger instance for this class
       */
      private static final Log log = ExoLogger.getLogger(QueryWorker.class);

      QueryWorker(Session s, String query, List<RepositoryException> exceptions, PrintWriter log)
      {
         this.s = s;
         this.query = query;
         this.exceptions = exceptions;

      }

      public void run()
      {
         try
         {
            // run the queries
            QueryManager qm = s.getWorkspace().getQueryManager();
            Query q = qm.createQuery(query, Query.XPATH);
            for (;;)
            {
               long time = System.currentTimeMillis();
               QueryResult result = q.execute();
               NodeIterator nodes = result.getNodes();
               long size = nodes.getSize();
               if (size == 0)
               {
                  while (nodes.hasNext())
                  {
                     size++;
                     Node node = nodes.nextNode();
                     log.info(node.getPath());
                  }
               }

               time = System.currentTimeMillis() - time;
               log.info(getName() + ": num nodes:" + size + " executed in: " + time + " ms.");

               count++;
               if (size % 5 != 0)
               {
                  exceptions.add(new RepositoryException("number of result nodes must be divisible by 5, but is: "
                           + size));
               }

               // do not consume all cpu power
               Thread.sleep(10);
               synchronized (this)
               {
                  if (finish)
                  {
                     break;
                  }
               }
            }
         }
         catch (RepositoryException e)
         {
            exceptions.add(e);
         }
         catch (InterruptedException e)
         {
            e.printStackTrace();
         }
         log.info("Executed " + count + " queries");
      }

      public synchronized void finish()
      {
         finish = true;
      }
   }
}
