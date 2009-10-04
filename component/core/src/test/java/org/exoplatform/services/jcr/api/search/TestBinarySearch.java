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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 22.10.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBinarySearch.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestBinarySearch extends JcrAPIBaseTest
{

   public void testSearchBinaryContent() throws Exception
   {
      Node rootNode = session.getRootNode();
      Node queryNode = rootNode.addNode("queryNode", "nt:unstructured");
      if (!queryNode.canAddMixin("rma:record"))
         throw new RepositoryException("Cannot add mixin node");
      else
      {
         queryNode.addMixin("rma:record");
         queryNode.setProperty("rma:recordIdentifier", "testIdentificator");
         queryNode.setProperty("rma:originatingOrganization", "testProperty2");
      }

      Node node1 = queryNode.addNode("Test1", "nt:file");
      Node content1 = node1.addNode("jcr:content", "nt:resource");
      content1.setProperty("jcr:lastModified", Calendar.getInstance());
      content1.setProperty("jcr:mimeType", "text/plain");
      content1.setProperty("jcr:data", new ByteArrayInputStream("ABBA AAAA".getBytes()));
      node1.addMixin("rma:record");
      node1.setProperty("rma:recordIdentifier", "testIdentificator");
      node1.setProperty("rma:originatingOrganization", "testProperty2");

      Node node2 = queryNode.addNode("Test2", "nt:file");
      Node content2 = node2.addNode("jcr:content", "nt:resource");
      content2.setProperty("jcr:lastModified", Calendar.getInstance());
      content2.setProperty("jcr:mimeType", "text/plain");
      content2.setProperty("jcr:data", new ByteArrayInputStream("ACDC EEEE".getBytes()));
      node2.addMixin("rma:record");
      node2.setProperty("rma:recordIdentifier", "testIdentificator");
      node2.setProperty("rma:originatingOrganization", "testProperty2");

      session.save();

      SessionImpl querySession = (SessionImpl)repository.login(credentials, "ws");
      String sqlQuery = "SELECT * FROM rma:record WHERE jcr:path LIKE '/queryNode/%' ";
      QueryManager manager = querySession.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertTrue(iter.getSize() == 2); // check target nodes for existanse
      while (iter.hasNext())
      {
         assertNotNull(iter.nextNode());
      }

      sqlQuery = "//*[jcr:contains(., 'ABBA')]";
      query = manager.createQuery(sqlQuery, Query.XPATH);

      queryResult = query.execute();
      iter = queryResult.getNodes();
      
      while(iter.hasNext()){
         System.out.print(iter.nextNode().getPath());
      }
      
      assertEquals("Result nodes count is wrong", 1, iter.getSize());
      while (iter.hasNext())
      {
         assertEquals("Content must be equals", "ABBA AAAA", iter.nextNode().getProperty("jcr:data").getString());
      }
   }

   public void testSearchBinaryContentAnotherSessionQueryManader() throws Exception
   {
      SessionImpl querySession = (SessionImpl)repository.login(credentials, "ws");

      Node rootNode = session.getRootNode();
      Node queryNode = rootNode.addNode("queryNode", "nt:unstructured");

      if (!queryNode.canAddMixin("rma:record"))
         throw new RepositoryException("Cannot add mixin node");
      else
      {
         queryNode.addMixin("rma:record");
         queryNode.setProperty("rma:recordIdentifier", "testIdentificator");
         queryNode.setProperty("rma:originatingOrganization", "testProperty2");
      }

      Node node1 = queryNode.addNode("Test1", "nt:file");
      Node content1 = node1.addNode("jcr:content", "nt:resource");
      content1.setProperty("jcr:lastModified", Calendar.getInstance());
      content1.setProperty("jcr:mimeType", "text/plain");
      content1.setProperty("jcr:data", new ByteArrayInputStream("ABBA AAAA".getBytes()));
      node1.addMixin("rma:record");
      node1.setProperty("rma:recordIdentifier", "testIdentificator");
      node1.setProperty("rma:originatingOrganization", "testProperty2");

      Node node2 = queryNode.addNode("Test2", "nt:file");
      Node content2 = node2.addNode("jcr:content", "nt:resource");
      content2.setProperty("jcr:lastModified", Calendar.getInstance());
      content2.setProperty("jcr:mimeType", "text/plain");
      content2.setProperty("jcr:data", new ByteArrayInputStream("ACDC EEEE".getBytes()));
      node2.addMixin("rma:record");
      node2.setProperty("rma:recordIdentifier", "testIdentificator");
      node2.setProperty("rma:originatingOrganization", "testProperty2");

      session.save();

      String sqlQuery = "SELECT * FROM rma:record WHERE jcr:path LIKE '/queryNode/%' ";
      QueryManager manager = querySession.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertTrue(iter.getSize() == 2); // check target nodes for existanse
      while (iter.hasNext())
      {
         assertNotNull(iter.nextNode());
      }

      sqlQuery = "//*[jcr:contains(., 'ABBA')]";
      query = manager.createQuery(sqlQuery, Query.XPATH);

      queryResult = query.execute();
      iter = queryResult.getNodes();

      assertEquals("Result nodes count is wrong", 1, iter.getSize());
      while (iter.hasNext())
      {
         assertEquals("Content must be equals", "ABBA AAAA", iter.nextNode().getProperty("jcr:data").getString());
      }
   }

}
