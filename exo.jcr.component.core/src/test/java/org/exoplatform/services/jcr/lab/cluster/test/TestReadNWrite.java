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
package org.exoplatform.services.jcr.lab.cluster.test;

import junit.framework.AssertionFailedError;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestReadNWrite extends JcrAPIBaseTest
{
   private boolean stop = false;

   public static final String COUNT = "count";

   public static final String CONTENT = "Content";

   public static final String STATISTIC = "Statistic";

   private int threadWriterCount = 10;

   private int threadReaderCount = 20;

   private final CountDownLatch doneSignal = new CountDownLatch(threadReaderCount + threadWriterCount);

   private static final String[] words =
      new String[]{"private", "branch", "final", "string", "logging", "bottle", "property", "node", "repository",
         "exception", "cycle", "value", "index", "meaning", "strange", "words", "hello", "outline", "finest",
         "basetest", "writer"};

   public void testReadNWrite() throws Exception
   {
      log.info("Creating threads...");
      for (int i = 0; i < threadWriterCount; i++)
      {
         // create new thread and start it
         new Thread(new WriterTask(i), "Writer-Thread-" + (i + 1)).start();
         log.info("Writer-Thread-" + (i + 1) + " created and started.");
      }
      for (int i = 0; i < threadReaderCount; i++)
      {
         // create new thread and start it
         new Thread(new QueryTask(), "Reader-Thread-" + (i + 1)).start();
         log.info("Reader-Thread-" + (i + 1) + " created and started.");
      }

      // wait 4 minutes
      try
      {
         Thread.sleep(60000 * 4);
         /*
         synchronized (this)
         {
            wait();
         }
         */
      }
      catch (InterruptedException e)
      {
         log.error(e);
      }

      stop = true;
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
                  updateStatistic((Node)sessionLocal.getItem("/Thread" + id + "/" + STATISTIC), word);
                  // add actual node
                  createTree((Node)sessionLocal.getItem("/Thread" + id + "/" + CONTENT)).addNode(word);
                  sessionLocal.save();
                  log.info("Time : " + (System.currentTimeMillis() - time));
               }
               catch (RepositoryException e)
               {
                  log.error(e);
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
                  Thread.sleep(300);
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

   private class QueryTask implements Runnable
   {

      private Random random;

      public QueryTask() throws RepositoryException
      {
         random = new Random();
      }

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         while (!stop)
         {
            Session sessionLocal = null;
            try
            {
               // login
               CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
               sessionLocal = (SessionImpl)repository.login(credentials, "ws");
               // prepare nodes
               Node rootLocal = sessionLocal.getRootNode();
               Node threadNode = getRandomChild(rootLocal, "Thread*");
               if (threadNode != null)
               {
                  Node statisticNode = threadNode.getNode(STATISTIC);
                  Node wordNode = getRandomChild(statisticNode, "*");
                  if (wordNode != null)
                  {
                     String word = wordNode.getName();
                     Long count = wordNode.getProperty(COUNT).getLong();

                     try
                     {
                        Thread.sleep(2000);
                     }
                     catch (InterruptedException e1)
                     {
                     }
                     long time = System.currentTimeMillis();
                     QueryManager qman = sessionLocal.getWorkspace().getQueryManager();

                     Query q = qman.createQuery("SELECT * FROM nt:base WHERE fn:name() = '" + word + "'", Query.SQL);
                     QueryResult res = q.execute();
                     long sqlsize = res.getNodes().getSize();
                     try
                     {
                        assertTrue("Exp: " + count + "\t found:" + sqlsize, sqlsize >= count);
                        log.info("Size : " + sqlsize + " time : " + (System.currentTimeMillis() - time));
                     }
                     catch (AssertionFailedError e)
                     {
                        log.info("Error : " + e.getMessage() + " time : " + (System.currentTimeMillis() - time));
                     }
                  }
               }
            }
            catch (Exception e)
            {
               log.error("An error occurs", e);
            }
            finally
            {
               if (sessionLocal != null)
               {
                  sessionLocal.logout();
                  sessionLocal = null;
               }
            }
         }
         doneSignal.countDown();
      }

      private Node getRandomChild(Node parent, String pattern) throws RepositoryException
      {
         NodeIterator iterator = parent.getNodes(pattern);
         if (iterator.getSize() < 1)
         {
            return null;
         }
         int i = random.nextInt((int)iterator.getSize());
         iterator.skip(i);
         return iterator.nextNode();
      }
   }
}
