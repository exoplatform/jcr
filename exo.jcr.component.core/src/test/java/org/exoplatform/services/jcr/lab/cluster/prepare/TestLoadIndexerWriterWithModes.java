/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.lab.cluster.prepare;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestLoadIndexerWriter.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestLoadIndexerWriterWithModes extends JcrAPIBaseTest
{
   public static final String COUNT = "count";

   public static final String CONTENT = "Content";

   public static final String STATISTIC = "Statistic";

   private volatile boolean stop = false;

   private AtomicBoolean makeThemWait = new AtomicBoolean();

   private int threadCount = 10;

   private final CountDownLatch startSignal = new CountDownLatch(1);

   private final CountDownLatch doneSignal = new CountDownLatch(threadCount);

   private final CyclicBarrier barrier = new CyclicBarrier(threadCount);

   private volatile CountDownLatch goSignal;

   private static final String[] words =
      new String[]{"private", "branch", "final", "string", "logging", "bottle", "property", "node", "repository",
         "exception", "cycle", "value", "index", "meaning", "strange", "words", "hello", "outline", "finest",
         "basetest", "writer"};

   private WorkspaceStorageCache cache;
   
   public void testWrite() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      this.cache = (WorkspaceStorageCache)wsc.getComponent(WorkspaceStorageCache.class);
      log.info("Skip (y/n) :");
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String line = reader.readLine();
      if (!line.equals("y"))
      {
         log.info("Creating threads...");
         for (int i = 0; i < threadCount; i++)
         {
            // create new thread and start it
            new Thread(new WriterTask(i)).start();
            log.info("Thread#" + i + " created and started.");
         }
         startSignal.countDown();
         while (true)
         {
            log.info("Type s to stop and return to make the threads wait or to release them :");
            line = reader.readLine();
            if (line.equalsIgnoreCase("s"))
            {
               break;
            }
            else if (makeThemWait.get())
            {
               makeThemWait.set(false);
               goSignal.countDown();
            }
            else
            {
               goSignal = new CountDownLatch(1);
               makeThemWait.set(true);
            }
         }

         stop = true;
      }
      else
      {
         log.info("Wait for data");
      }

      doneSignal.await();
      System.exit(0);
   }

   private class WriterTask implements Runnable
   {

      private int id;

      private Random random;

      public WriterTask(int id) throws RepositoryException
      {
         this.id = id;
         // login
         CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
         Session sessionLocal = (SessionImpl)repository.login(credentials, "ws");
         // prepare nodes
         Node root = sessionLocal.getRootNode();
         Node threadNode = root.addNode("Thread" + id);
         threadNode.addNode(STATISTIC);
         threadNode.addNode(CONTENT);
         random = new Random();
         sessionLocal.save();
         sessionLocal.logout();
         sessionLocal = null;
      }

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         try
         {
            startSignal.await();
            while (!stop)
            {
               // get any word
               int i = random.nextInt(words.length);
               String word = words[i] + id; // "hello12" if thread#12 is creating it
               Session sessionLocal = null;
               try
               {
                  CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
                  sessionLocal = (SessionImpl)repository.login(credentials, "ws");                  
                  long time = System.currentTimeMillis();
                  // update statistic
                  updateStatistic((Node)sessionLocal.getItem("/Thread" + id + "/" + STATISTIC),word);
                  // add actual node
                  createTree((Node)sessionLocal.getItem("/Thread" + id + "/" + CONTENT)).addNode(word);
                  sessionLocal.save();
                  log.info("Time : " + (System.currentTimeMillis() - time));
               }
               catch (Exception e1)
               {
                  if (sessionLocal != null)
                  {
                     // discard session changes 
                     sessionLocal.refresh(false);                     
                  }
                  log.error("An error occurs", e1);
               }
               finally
               {
                  if (sessionLocal != null)
                  {
                     sessionLocal.logout();
                     sessionLocal = null;                  
                  }
               }
               try
               {
                  if (makeThemWait.get())
                  {
                     barrier.await();
                     log.info("The threads are waiting for the go signal, the current size of the cache is " + cache.getSize());
                     goSignal.await();
                  }
                  else
                  {
                     Thread.sleep(300);
                  }
               }
               catch (InterruptedException e)
               {
               }
            }
         }
         catch (Exception e)
         {
            log.error("An unexpected error happens", e);
         }
         finally
         {
            doneSignal.countDown();
         }
      }

      /**
       * increments property in JCR: "./statistic/[word].count"
       * 
       * @param word
       * @throws RepositoryException
       */
      private void updateStatistic(Node statisticNode, String word) throws RepositoryException
      {
         Node wordNode;
         long count = 0;
         if (statisticNode.hasNode(word))
         {
            wordNode = statisticNode.getNode(word);
            count = wordNode.getProperty(COUNT).getLong();
         }
         else
         {
            wordNode = statisticNode.addNode(word);
         }
         wordNode.setProperty(COUNT, count + 1);
      }

      /**
       * Created node tree like: "./content/n123456/n1234567/n12345678"
       * based on current time
       * 
       * @return
       * @throws RepositoryException
       */
      private Node createTree(Node contentNode) throws RepositoryException
      {
         // created node tree like: "./content/n123456/n1234567/n12345678"
         Node end;
         long time = System.currentTimeMillis();
         long child1 = time / 100000; // each 100s new node
         long child2 = time / 10000; // each 10s new node
         long child3 = time / 1000; // each 1s new node
         end = addOrCreate("n" + child1, contentNode);
         end = addOrCreate("n" + child2, end);
         end = addOrCreate("n" + child3, end);
         return end;
      }

      /**
       * Gets or creates node
       * 
       * @param name
       * @param parent
       * @return
       * @throws RepositoryException
       */
      private Node addOrCreate(String name, Node parent) throws RepositoryException
      {
         if (parent.hasNode(name))
         {
            return parent.getNode(name);
         }
         else
            return parent.addNode(name);
      }
   }

}
