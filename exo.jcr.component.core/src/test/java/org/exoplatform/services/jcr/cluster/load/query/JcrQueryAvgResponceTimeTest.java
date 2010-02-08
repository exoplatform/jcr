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
package org.exoplatform.services.jcr.cluster.load.query;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.cluster.load.AbstractAvgResponceTimeTest;
import org.exoplatform.services.jcr.cluster.load.AbstractTestAgent;
import org.exoplatform.services.jcr.cluster.load.NodeInfo;
import org.exoplatform.services.jcr.cluster.load.WorkerResult;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.jboss.cache.CacheException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class JcrQueryAvgResponceTimeTest extends JcrImplBaseTest
{

   /**
    * 2min default time of work of one iteration.
    */
   private static final int ITERATION_TIME = 60 * 1000;

   /**
    * How much thread will be added on the next iteration.
    */
   private static final int ITERATION_GROWING_POLL = 15;

   /**
    * Number between 0 and 100 show % how many read operations. 
    */
   private static final int READ_VALUE = 90;

   private static final String[] words =
      new String[]{"private", "branch", "final", "string", "logging", "bottle", "property", "node", "repository",
         "exception", "cycle", "value", "index", "meaning", "strange", "words", "hello", "outline", "finest",
         "basetest", "writer"};

   public static final String FIELDNAME_COUNT = "count";

   public static final String FIELDNAME_CONTENT = "Content";

   public static final String FIELDNAME_STATISTIC = "Statistic";

   private static final String TEST_ROOT = "JcrQueryAvgResponceTimeTest";

   public void testname() throws Exception
   {
      QueryAvgResponceTimeTest test =
         new QueryAvgResponceTimeTest(repository, ITERATION_GROWING_POLL, ITERATION_TIME, 5, READ_VALUE);
      test.testResponce();

   }

   private class QueryAvgResponceTimeTest extends AbstractAvgResponceTimeTest
   {

      private final RepositoryImpl repository;

      /**
       * @param iterationGrowingPoll
       * @param iterationTime
       * @param initialSize
       * @param readValue
       */
      public QueryAvgResponceTimeTest(RepositoryImpl repository, int iterationGrowingPoll, int iterationTime,
         int initialSize, int readValue)
      {
         super(iterationGrowingPoll, iterationTime, initialSize, readValue);
         this.repository = repository;
      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractAvgResponceTimeTest#getAgent(java.util.List, java.util.List, java.util.concurrent.CountDownLatch, int, java.util.Random)
       */
      @Override
      protected AbstractTestAgent getAgent(List<NodeInfo> nodesPath, List<WorkerResult> responceResults,
         CountDownLatch startSignal, int readValue, Random random)
      {
         return new QueryTestAgent(repository, nodesPath, responceResults, startSignal, readValue, random);
      }

   }

   private class QueryTestAgent extends AbstractTestAgent
   {
      private final RepositoryImpl repository;

      private UUID threadUUID;

      /**
       * @param repository 
       * @param nodesPath
       * @param responceResults
       * @param startSignal
       * @param READ_VALUE
       * @param random
       */
      public QueryTestAgent(RepositoryImpl repository, List<NodeInfo> nodesPath, List<WorkerResult> responceResults,
         CountDownLatch startSignal, int readValue, Random random)
      {
         super(nodesPath, responceResults, startSignal, readValue, random);
         this.threadUUID = UUID.randomUUID();
         this.repository = repository;
         initRoot();
      }

      /**
       * @param repository
       * @throws LoginException
       * @throws NoSuchWorkspaceException
       * @throws RepositoryException
       * @throws ItemExistsException
       * @throws PathNotFoundException
       * @throws VersionException
       * @throws ConstraintViolationException
       * @throws LockException
       * @throws AccessDeniedException
       * @throws InvalidItemStateException
       * @throws NoSuchNodeTypeException
       */
      private void initRoot()
      {
         int maxAttempts = 10;
         CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
         for (int i = 0; i < maxAttempts; i++)
         {
            boolean isSuccessful = false;
            Session sessionLocal = null;
            try
            {

               sessionLocal = repository.login(credentials, "ws");
               // prepare nodes
               Node wsRoot = sessionLocal.getRootNode();
               Node threadNode = getOrCreateNode(getOrCreateNode(TEST_ROOT, wsRoot), threadUUID);
               sessionLocal.save();
               sessionLocal.logout();
               sessionLocal = null;
               isSuccessful = true;
            }
            catch (CacheException e)
            {
               log.error("error on creating root attempt " + i + " from " + maxAttempts);
               //ignore
            }
            catch (RepositoryException e)
            {
               log.error("error on creating root attempt " + i + " from " + maxAttempts);
            }
            finally
            {
               if (sessionLocal != null)
               {
                  try
                  {
                     sessionLocal.refresh(false);
                     sessionLocal.logout();
                  }
                  catch (RepositoryException e)
                  {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                  }
               }
            }
            if (isSuccessful)
            {
               break;
            }
         }

      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doRead(java.util.List)
       */
      @Override
      public List<WorkerResult> doRead(List<NodeInfo> nodesPath)
      {
         List<WorkerResult> result = new ArrayList<WorkerResult>();
         Session sessionLocal = null;
         try
         {
            // login
            CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());

            sessionLocal = repository.login(credentials, "ws");
            Node testRoot = sessionLocal.getRootNode().getNode(TEST_ROOT);
            // prepare nodes
            int i = random.nextInt(words.length);
            String word = words[i];
            Query q =
               sessionLocal.getWorkspace().getQueryManager().createQuery(
                  "SELECT * FROM nt:base WHERE " + FIELDNAME_CONTENT + "='" + word + "' AND jcr:path LIKE '"
                     + testRoot.getPath() + "/%'", Query.SQL);
            long start = System.currentTimeMillis();
            QueryResult res = q.execute();
            long sqlsize = res.getNodes().getSize();
            result.add(new WorkerResult(true, System.currentTimeMillis() - start));
            //log.info(word + " found:" + sqlsize + " time=" + (System.currentTimeMillis() - start));

         }
         catch (Exception e)
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
         return result;
      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractTestAgent#doWrite(java.util.List)
       */
      @Override
      public List<WorkerResult> doWrite(List<NodeInfo> nodesPath)
      {
         List<WorkerResult> result = new ArrayList<WorkerResult>();
         // get any word
         int i = random.nextInt(words.length);
         String word = words[i];

         Session sessionLocal = null;
         try
         {
            CredentialsImpl credentials = new CredentialsImpl("admin", "admin".toCharArray());
            sessionLocal = repository.login(credentials, "ws");
            long start = System.currentTimeMillis();
            Node threadNode = getOrCreateNode(getOrCreateNode(TEST_ROOT, sessionLocal.getRootNode()), threadUUID);
            addCountent(threadNode, UUID.randomUUID(), word);
            sessionLocal.save();
            result.add(new WorkerResult(false, System.currentTimeMillis() - start));
            //log.info(word + " time : " + (System.currentTimeMillis() - start));
         }
         catch (Exception e1)
         {
            if (sessionLocal != null)
            {
               // discard session changes 
               try
               {
                  sessionLocal.refresh(false);
               }
               catch (RepositoryException e)
               {
                  log.error("An error occurs", e);
               }
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
         return result;
      }

      private void addCountent(Node testRoot, UUID nodePath, String content) throws RepositoryException
      {
         Node l5 = getOrCreateNode(testRoot, nodePath);
         l5.setProperty(FIELDNAME_CONTENT, content);
      }

      private Node getOrCreateNode(Node testRoot, UUID nodePath) throws RepositoryException
      {
         String uuidPath = nodePath.toString();
         Node l1 = getOrCreateNode(uuidPath.substring(0, 8), testRoot);
         Node l2 = getOrCreateNode(uuidPath.substring(9, 13), l1);
         Node l3 = getOrCreateNode(uuidPath.substring(14, 18), l2);
         Node l4 = getOrCreateNode(uuidPath.substring(19, 23), l3);
         return getOrCreateNode(uuidPath.substring(24), l4);

      }

      /**
       * Gets or creates node
       * 
       * @param name
       * @param parent
       * @return
       * @throws RepositoryException
       */
      private Node getOrCreateNode(String name, Node parent) throws RepositoryException
      {
         if (parent.hasNode(name))
         {
            return parent.getNode(name);
         }
         return parent.addNode(name);
      }
   }
}
