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
package org.exoplatform.services.jcr.load.perf;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryResultImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestLimitAndOffset.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestLimitAndOffset extends BaseStandaloneTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.TestLimitAndOffset");

   private QueryImpl query;

   private final static long NODES_COUNT = 2000;

   private final static long NEED_NODES = 10;

   private final static String testRootName = "testRoot";

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   public void setUp() throws Exception
   {
      super.setUp();
      Node testRoot = root.addNode(testRootName);
      for (int i = 0; i < NODES_COUNT; i++)
      {
         Node newNode = testRoot.addNode("Node" + i);
         newNode.setProperty("val", i);

      }
      session.save();
      query = createXPathQuery("/jcr:root/" + testRootName + "/* order by @val");

   }

   private QueryImpl createXPathQuery(String xpath) throws InvalidQueryException, RepositoryException
   {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      return (QueryImpl)queryManager.createQuery(xpath, Query.XPATH);
   }

   protected void checkResult(QueryResult result, Node[] expectedNodes) throws RepositoryException
   {
      assertEquals(expectedNodes.length, result.getNodes().getSize());
   }

   public void testGetNodeIterator() throws Exception
   {
      Node testRoot = root.getNode(testRootName);
      long time = System.currentTimeMillis();
      NodeIterator nodeIterartor = testRoot.getNodes();
      long size = nodeIterartor.getSize();
      assertEquals(NODES_COUNT, size);

      for (int i = 0; i < NEED_NODES; i++)
      {
         Node node = nodeIterartor.nextNode();
         assertEquals(i, node.getProperty("val").getLong());
      }
      log.info("Time testGetNodeIterator=" + (System.currentTimeMillis() - time));
   }

   public void testQuery() throws Exception
   {
      QueryResult result = query.execute();

      long time = System.currentTimeMillis();
      NodeIterator nodeIterartor = result.getNodes();
      long size = nodeIterartor.getSize();
      assertEquals(NODES_COUNT, ((QueryResultImpl)result).getTotalSize());

      for (int i = 0; i < NEED_NODES; i++)
      {
         Node node = nodeIterartor.nextNode();
         assertEquals(i, node.getProperty("val").getLong());
      }
      log.info("Time testQuery =" + (System.currentTimeMillis() - time));
   }

   public void testQeryLimitAndOffset() throws Exception
   {
      query.setLimit(NEED_NODES);
      QueryResult result = query.execute();

      long time = System.currentTimeMillis();
      NodeIterator nodeIterartor = result.getNodes();
      long size = nodeIterartor.getSize();
      assertEquals(NODES_COUNT, ((QueryResultImpl)result).getTotalSize());
      assertEquals(NEED_NODES, size);

      for (int i = 0; i < NEED_NODES; i++)
      {
         Node node = nodeIterartor.nextNode();
         assertEquals(i, node.getProperty("val").getLong());
      }
      log.info("Time testQeryLimitAndOffset=" + (System.currentTimeMillis() - time));
   }

   public void testGetNodeIteratorSecondPage() throws Exception
   {
      Node testRoot = root.getNode(testRootName);
      long time = System.currentTimeMillis();
      NodeIterator nodeIterartor = testRoot.getNodes();
      long size = nodeIterartor.getSize();
      assertEquals(NODES_COUNT, size);
      nodeIterartor.skip(NEED_NODES);
      for (int i = 0; i < NEED_NODES; i++)
      {
         Node node = nodeIterartor.nextNode();
         assertEquals(i + NEED_NODES, node.getProperty("val").getLong());
      }
      log.info("Time _testGetNodeIteratorSecondPage=" + (System.currentTimeMillis() - time));
   }

   public void testQuerySecondPage() throws Exception
   {
      QueryResult result = query.execute();

      long time = System.currentTimeMillis();
      NodeIterator nodeIterartor = result.getNodes();
      long size = nodeIterartor.getSize();
      assertEquals(NODES_COUNT, size);
      nodeIterartor.skip(NEED_NODES);
      for (int i = 0; i < NEED_NODES; i++)
      {
         Node node = nodeIterartor.nextNode();
         assertEquals(i + NEED_NODES, node.getProperty("val").getLong());
      }
      log.info("Time _testQuerySecondPage =" + (System.currentTimeMillis() - time));
   }
}
