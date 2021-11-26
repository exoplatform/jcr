/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.query.lucene.TwoWayRangeIterator;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestTwoWayRangeIterator extends JcrImplBaseTest
{
   private Random random = new Random();
   
   private Session userSession;

   private final int TEST_NODES_COUNT = 100;

   private final int ATTEMPTS = 20;

   private String testRootNodeName = "testRoot";

   public void prepareRoot(Node testRoot) throws RepositoryException
   {
      for (int i = 0; i < TEST_NODES_COUNT; i++)
      {
         Node subnode = testRoot.addNode("TestNode" + String.format("%07d", i));
         subnode.setProperty("val", i);
         ExtendedNode subnode2 = (ExtendedNode)testRoot.addNode("TestNode2-" + String.format("%07d", i));
         subnode2.setProperty("val", i);
         subnode2.addMixin("exo:privilegeable");
         HashMap<String, String[]> perm = new HashMap<String, String[]>();
         perm.put("admin", PermissionType.ALL);
         subnode2.setPermissions(perm);
      }

   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      Node testRoot = root.addNode(testRootNodeName);
      prepareRoot(testRoot);
      root.save();
      userSession = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
   }
   
   @Override
   protected void tearDown() throws Exception
   {
      userSession.logout();
      super.tearDown();
   }

   // Check random skipping from start of set
   public void testSkipFromStart() throws Exception
   {

      for (int i = 0; i < ATTEMPTS; i++)
      {

         final int skip = random.nextInt(TEST_NODES_COUNT - 2) + 1;
         ScoreNodeTester tester = new ScoreNodeTester()
         {

            public TwoWayRangeIterator execute(Query query) throws RepositoryException
            {
               QueryResult result = query.execute();
               TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
               iterator.skip(skip);
               return iterator;
            }

         };
         checkPosition(tester, skip);
      }
   }

   // Test skip zero.
   public void testSkipZero() throws RepositoryException
   {
      ScoreNodeTester tester = new ScoreNodeTester()
      {

         public TwoWayRangeIterator execute(Query query) throws RepositoryException
         {
            QueryResult result = query.execute();
            TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
            iterator.skip(0);
            return iterator;
         }

      };
      checkPosition(tester, 0);
   }

   /**
    * Test NoSuchElementException
    * 
    * @throws Exception
    */
   public void testSkipBeforeFirst() throws Exception
   {

      for (int i = 0; i < ATTEMPTS; i++)
      {

         final int skip = random.nextInt(TEST_NODES_COUNT - 2) + 1;

         ScoreNodeTester tester = new ScoreNodeTester()
         {

            public TwoWayRangeIterator execute(Query query) throws RepositoryException
            {
               QueryResult result = query.execute();
               TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
               iterator.skip(skip);
               assertEquals(skip, iterator.getPosition());
               try
               {
                  int skip2 = skip + 1 + random.nextInt(TEST_NODES_COUNT);
                  iterator.skipBack(skip2);
                  fail();
               }
               catch (NoSuchElementException e)
               {
                  // ok
               }
               return iterator;
            }

         };
         checkPosition(tester, skip);
      }
   }

   /**
    * Test IllegalArgumentException
    * 
    * @throws Exception
    */
   public void testSkipNegative() throws Exception
   {

      for (int i = 0; i < ATTEMPTS; i++)
      {

         final int skip = random.nextInt(TEST_NODES_COUNT - 2) + 1;

         ScoreNodeTester tester = new ScoreNodeTester()
         {

            public TwoWayRangeIterator execute(Query query) throws RepositoryException
            {
               QueryResult result = query.execute();
               TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
               iterator.skip(skip);
               assertEquals(skip, iterator.getPosition());
               try
               {
                  iterator.skipBack(-1 * skip);
                  fail();
               }
               catch (IllegalArgumentException e)
               {
                  // ok

               }
               return iterator;
            }

         };
         checkPosition(tester, skip);
      }
   }

   /**
    * Test skip to first element
    * 
    * @throws Exception
    */
   public void testSkipAtFirst() throws Exception
   {

      for (int i = 0; i < ATTEMPTS; i++)
      {

         final int skip = random.nextInt(TEST_NODES_COUNT - 2) + 1;

         ScoreNodeTester tester = new ScoreNodeTester()
         {

            public TwoWayRangeIterator execute(Query query) throws RepositoryException
            {
               QueryResult result = query.execute();
               TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
               iterator.skip(skip);
               assertEquals(skip, iterator.getPosition());
               iterator.skipBack(skip);
               return iterator;
            }

         };
         checkPosition(tester, 0);
      }
   }

   /**
    * Test random skip back
    * 
    * @throws Exception
    */
   public void testRandomSkipBack() throws Exception
   {
      for (int i = 0; i < ATTEMPTS; i++)
      {
         final int skip = random.nextInt(TEST_NODES_COUNT - 10);
         ScoreNodeTester tester = new ScoreNodeTester()
         {

            public TwoWayRangeIterator execute(Query query) throws RepositoryException
            {
               QueryResult result = query.execute();
               TwoWayRangeIterator iterator = (TwoWayRangeIterator)result.getNodes();
               iterator.skip(5);
               iterator.skip(skip);
               iterator.skipBack(skip);
               return iterator;
            }

         };
         checkPosition(tester, 5);
      }
   }

   protected void checkPosition(ScoreNodeTester testAction, long expectedPosition) throws RepositoryException
   {

      QueryManager qm = userSession.getWorkspace().getQueryManager();

      // Doc order
      String strDocOrder = "select * from nt:unstructured where jcr:path like '/" + testRootNodeName + "/%'";

      Query selectChildQuery = qm.createQuery(strDocOrder, Query.SQL);
      TwoWayRangeIterator iterator = testAction.execute(selectChildQuery);
      assertTrue(iterator.hasNext());
      assertEquals(expectedPosition, iterator.getPosition());
      assertEquals(expectedPosition, ((Node)iterator.next()).getProperty("val").getLong());

      // Order by
      String strOrder = "select * from nt:unstructured where jcr:path like '/" + testRootNodeName + "/%' order by val";

      Query selectOrderChildQuery = qm.createQuery(strOrder, Query.SQL);
      TwoWayRangeIterator orderIterator = testAction.execute(selectOrderChildQuery);
      assertTrue(orderIterator.hasNext());
      assertEquals(expectedPosition, orderIterator.getPosition());
      Node nextNode = (Node)orderIterator.next();

      if (nextNode.getProperty("val").getLong() != expectedPosition)
      {
         orderIterator.skipBack(orderIterator.getPosition());
         while (orderIterator.hasNext())
         {
            Node nooode = (Node)orderIterator.next();
         }
      }
      assertEquals(expectedPosition, nextNode.getProperty("val").getLong());

   }

   private interface ScoreNodeTester
   {
      public TwoWayRangeIterator execute(Query query) throws RepositoryException;
   }

}
