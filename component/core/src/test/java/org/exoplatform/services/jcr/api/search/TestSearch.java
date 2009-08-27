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
package org.exoplatform.services.jcr.api.search;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 25.12.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSearch.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestSearch
   extends JcrAPIBaseTest
{

   private Node testNode;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testNode = session.getRootNode().addNode("searchTestNode");
      session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testNode.remove();
      session.save();

      super.tearDown();
   }

   private Node addNtFile(Node parent, InputStream content) throws Exception
   {
      Node node1 = parent.addNode("File", "nt:file");
      Node ntFile = node1.addNode("jcr:content", "nt:resource");
      ntFile.setProperty("jcr:lastModified", Calendar.getInstance());
      ntFile.setProperty("jcr:mimeType", "text/plain");
      ntFile.setProperty("jcr:data", content);

      return ntFile;
   }

   public void testAllofNodeType() throws Exception
   {

      for (int i = 1; i <= 100; i++)
      {
         Node queryNode = testNode.addNode("node-" + 1, "exojcrtest:type1");
         Node file = addNtFile(queryNode, new ByteArrayInputStream("ACDC EEEE".getBytes()));
      }
      session.save();

      String sqlQuery = "SELECT * FROM exojcrtest:type1 WHERE jcr:path LIKE '" + testNode.getPath() + "/%'";
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertTrue(iter.getSize() == 100); // check target nodes for existanse
      while (iter.hasNext())
      {
         assertNotNull(iter.nextNode());
      }
   }

   public void testAllofNodeTypeWithOrder() throws Exception
   {

      for (int i = 1; i <= 100; i++)
      {
         Node queryNode = testNode.addNode("node-" + 1, "exojcrtest:type1");
         Node file = addNtFile(queryNode, new ByteArrayInputStream("ACDC EEEE".getBytes()));
      }
      session.save();

      String sqlQuery =
               "SELECT * FROM exojcrtest:type1 WHERE jcr:path LIKE '" + testNode.getPath()
                        + "/%' order by jcr:primaryType";
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertTrue(iter.getSize() == 100); // check target nodes for existense
      while (iter.hasNext())
      {
         assertNotNull(iter.nextNode());
      }
   }
}
