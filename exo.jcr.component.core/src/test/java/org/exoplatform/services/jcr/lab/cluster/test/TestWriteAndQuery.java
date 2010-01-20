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

import junit.framework.AssertionFailedError;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.lab.cluster.prepare.TestLoadIndexerWriter;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestLoadIndexerQuery.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestWriteAndQuery extends JcrAPIBaseTest
{
   public static final String COUNT = "count";

   public static final String CONTENT = "Content";

   public static final String STATISTIC = "Statistic";

   private boolean stop = false;

   private int threadCount = 10;

   private static final String[] words =
      new String[]{"private", "branch", "final", "string", "logging", "bottle", "property", "node", "repository",
         "exception", "cycle", "value", "index", "meaning", "strange", "words", "hello", "outline", "finest",
         "basetest", "writer"};

   public void testQuery() throws RepositoryException
   {
      log.info("Creating threads...");
      for (int i = 0; i < threadCount; i++)
      {
         // create new thread and start it
         new Thread(new WriterTask(i)).start();
         log.info("Write Thread#" + i + " created and started.");
         // create new thread and start it
         new Thread(new QueryTask()).start();
         log.info("Query Thread#" + i + " created and started.");
      }

      // wait 4 minutes
      try
      {
         Thread.sleep(60000 * 4);
      }
      catch (InterruptedException e)
      {
         log.error(e);
      }

      stop = true;
   }

   private class QueryTask implements Runnable
   {
      private SessionImpl sessionLocal;

      private Node rootLocal;

      private Random random;

      public QueryTask() throws RepositoryException
      {
         // login
         CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
         sessionLocal = (SessionImpl)repository.login(credentials, "ws");
         // prepare nodes
         rootLocal = sessionLocal.getRootNode();
         random = new Random();
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
               Node threadNode = getRandomChild(rootLocal, "Thread*");
               if (threadNode != null)
               {
                  Node statisticNode = threadNode.getNode(TestLoadIndexerWriter.STATISTIC);
                  Node wordNode = getRandomChild(statisticNode, "*");
                  if (wordNode != null)
                  {
                     String word = wordNode.getName();
                     Long count = wordNode.getProperty(TestLoadIndexerWriter.COUNT).getLong();

                     QueryManager qman = sessionLocal.getWorkspace().getQueryManager();

                     Query q = qman.createQuery("SELECT * FROM nt:base WHERE fn:name() = '" + word + "'", Query.SQL);
                     QueryResult res = q.execute();
                     long sqlsize = res.getNodes().getSize();
                     try
                     {
                        assertTrue("Exp: " + count + "\t found:" + sqlsize, sqlsize >= count);
                        System.out.print("+(" + sqlsize + ")");
                     }
                     catch (AssertionFailedError e)
                     {
                        System.out.println("-" + e.getMessage());
                     }
                  }
               }
            }

         }
         catch (RepositoryException e)
         {
            log.error(e);
         }
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

   private class WriterTask implements Runnable
   {

      private int id;

      private SessionImpl sessionLocal;

      private Node statisticNode;

      private Node contentNode;

      private Random random;

      public WriterTask(int id) throws RepositoryException
      {
         this.id = id;
         // login
         CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
         sessionLocal = (SessionImpl)repository.login(credentials, "ws");
         // prepare nodes
         Node root = sessionLocal.getRootNode();
         Node threadNode = root.addNode("Thread" + id);
         statisticNode = threadNode.addNode(STATISTIC);
         contentNode = threadNode.addNode(CONTENT);
         random = new Random();
         sessionLocal.save();
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
               // update statistic
               updateStatistic(word);
               // add actual node
               createTree().addNode(word);
               sessionLocal.save();
               System.out.print("#");

               try
               {
                  Thread.sleep(300);
               }
               catch (InterruptedException e)
               {
               }
            }
         }
         catch (RepositoryException e)
         {
            log.error(e);
         }
      }

      /**
       * increments property in JCR: "./statistic/[word].count"
       * 
       * @param word
       * @throws RepositoryException
       */
      private void updateStatistic(String word) throws RepositoryException
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
      private Node createTree() throws RepositoryException
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
