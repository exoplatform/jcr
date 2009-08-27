/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.usecases.query;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS Author : Ly Dinh Quang quang.ly@exoplatform.com xxx5669@yahoo.com
 * Jul 23, 2008
 */
public class TestQueryWithNumberAndSpace
   extends BaseUsecasesTest
{

   /**
    * Test of number as path in query.
    * 
    * @throws Exception
    */
   public void testQuery1() throws Exception
   {
      System.out.println("\n\n----------Test query contains number\n\n");
      Node document = root.addNode("Document", "nt:unstructured");
      Node testNode1 = document.addNode("Test1", "nt:unstructured");
      Node testNode11 = testNode1.addNode("Test11", "nt:file");
      Node content1 = testNode11.addNode("jcr:content", "nt:resource");
      content1.setProperty("jcr:lastModified", Calendar.getInstance());
      content1.setProperty("jcr:mimeType", "text/xml");
      content1.setProperty("jcr:data", "");

      Node testNode2 = document.addNode("2008", "nt:unstructured");
      Node testNode21 = testNode2.addNode("Test21", "nt:file");
      Node content2 = testNode21.addNode("jcr:content", "nt:resource");
      content2.setProperty("jcr:lastModified", Calendar.getInstance());
      content2.setProperty("jcr:mimeType", "text/xml");
      content2.setProperty("jcr:data", "");
      session.save();

      Query query = null;
      QueryResult result = null;
      NodeIterator iterate = null;

      QueryManager queryManager = session.getWorkspace().getQueryManager();

      query = queryManager.createQuery("/jcr:root/Document/element(Test1, nt:unstructured)", Query.XPATH);
      result = query.execute();
      iterate = result.getNodes();
      assertEquals(1, iterate.getSize());

      query = queryManager.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/Document/2008'", Query.SQL);
      result = query.execute();
      assertEquals(1, result.getNodes().getSize());

      try
      {
         query = queryManager.createQuery("/jcr:root/document/element(2008, nt:unstructured)", Query.XPATH);
         result = query.execute();
         fail(); // there must be InvalidQueryException - XPATH do not support numbers in path
      }
      catch (InvalidQueryException e)
      {
         // correct
      }

      // remove data
      document.remove();
      session.save();
   }

   /**
    * Test of string with whitespace as path in query.
    * 
    * @throws Exception
    */
   public void testQuery2() throws Exception
   {
      System.out.println("\n\n----------Test Query contains space\n\n");
      Node document = root.addNode("Document", "nt:unstructured");
      Node testNode3 = document.addNode("test A", "nt:unstructured");
      Node testNode31 = testNode3.addNode("Test31", "nt:file");
      Node content3 = testNode31.addNode("jcr:content", "nt:resource");
      content3.setProperty("jcr:lastModified", Calendar.getInstance());
      content3.setProperty("jcr:mimeType", "text/xml");
      content3.setProperty("jcr:data", "");
      session.save();

      QueryManager queryManager = session.getWorkspace().getQueryManager();

      Query query =
               queryManager.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/Document/test A'",
                        Query.SQL);
      QueryResult result = query.execute();
      assertEquals(1, result.getNodes().getSize());

      try
      {
         query = queryManager.createQuery("/jcr:root/document/element('test A', nt:unstructured)", Query.XPATH);
         result = query.execute();
         NodeIterator iterate = result.getNodes();
         assertEquals(1, iterate.getSize());
      }
      catch (InvalidQueryException e)
      {
         // correct
      }

      // remove data
      document.remove();
      session.save();
   }
}
