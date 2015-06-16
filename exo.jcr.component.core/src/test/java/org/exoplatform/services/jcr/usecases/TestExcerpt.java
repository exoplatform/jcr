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
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Created by The eXo Platform SAS Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@yahoo.com Aug 8, 2008
 */
public class TestExcerpt extends BaseUsecasesTest
{
   private String s1 =
      "Additionally there is a parameter that controls the format of the excerpt created. "
         + "In JCR 1.9 the default is set to"
         + "org.exoplatform.services.jcr.impl.core.query.lucene.DefaultHTMLExcerpt. "
         + "the configuration parameter for this setting is:" + "This is the test for Excerpt query";

   private String string1_excerpt =
      "<div><span>Additionally there is a parameter that controls the format of the "
         + "<strong>excerpt</strong> created. In JCR 1.9 the default is set ...</span><span>"
         + "the configuration parameter for this setting is:This is the test for "
         + "<strong>Excerpt</strong> query</span></div>";

   private String s2 =
      "It is a test for excerpt query.Searching with synonyms is integrated in the jcr:contains() "
         + "function and uses the same syntax " + "like synonym searches with Google. If a search "
         + "term is prefixed with ~ also synonyms of the search term are considered. Example:";

   private String string2_excerpt =
      "<div><span>It is a test for <strong>excerpt</strong> query.Searching with synonyms is integrated in the jcr:contains() function and uses the same syntax like synonym searches ...</span></div>";

   private String s3 = "JCR supports such features as Lucene Fuzzy Searches";
   
   private String string3_excerpt= "<div><span></span></div>";

   private Session testSession;

   private Node testRoot;

   /**
    * Initialization flag.
    */
   private static boolean isInitialized = false;

   @Override
   public void initRepository() throws RepositoryException
   {
      super.initRepository();
      if (!isInitialized)
      {
         ExtendedNodeTypeManager ntManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
         InputStream is = TestExcerpt.class.getResourceAsStream("/nodetypes/ext-registry-nodetypes.xml");

         ntManager.registerNodeTypes(is, ExtendedNodeTypeManager.REPLACE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
         ntManager.registerNodeTypes(TestExcerpt.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config.xml"), 0,
            NodeTypeDataManager.TEXT_XML);
         ntManager.registerNodeTypes(TestExcerpt.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config-extended.xml"), 0,
            NodeTypeDataManager.TEXT_XML);

         isInitialized = true;
      }
   }

   /**
    * @see org.exoplatform.services.jcr.BaseStandaloneTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      // TODO Auto-generated method stub
      super.setUp();
      ManageableRepository db1tckRepo = repositoryService.getRepository("db1tck");
      assertNotNull(db1tckRepo);
      testSession = db1tckRepo.login(credentials, "ws2");
      testRoot = testSession.getRootNode();
   }

   public void testExcerpt() throws Exception
   {
      for (int z = 0; z < 1; z++)
      {

         Node excerptTest = testRoot.addNode("testExcerpt");

         Node node1 = excerptTest.addNode("Node1", "exo:article");
         node1.setProperty("exo:title", "");
         node1.setProperty("exo:text", s1);

         Node node2 = excerptTest.addNode("Node2", "exo:article");
         node2.setProperty("exo:title", "");
         node2.setProperty("exo:text", s2);

         Node node3 = excerptTest.addNode("Node3", "exo:article");
         node3.setProperty("exo:title", "");
         node3.setProperty("exo:text", s3);

         testSession.save();

         QueryManager queryManager = testSession.getWorkspace().getQueryManager();
         Query q1 =
            queryManager.createQuery("select exo:text, excerpt(.) from exo:article where jcr:path LIKE '"
               + excerptTest.getPath() + "/%' and contains(., 'excerpt') ORDER BY exo:title", Query.SQL);
         for (int i = 0; i < 1; i++)
         {
            checkResult(q1);
         }

         Query q2 =
            queryManager.createQuery("/jcr:root/" + excerptTest.getPath()
               + "//*[jcr:contains(., 'excerpt')]/(@exo:text|rep:excerpt(.)) order by @exo:title", Query.XPATH);
         for (int i = 0; i < 1; i++)
         {
            checkResult(q2);
         }
         excerptTest.remove();
         testSession.save();
      }
   }

   private void checkResult(Query query) throws RepositoryException
   {
      QueryResult result2 = query.execute();
      RowIterator rows = result2.getRows();
      assertEquals(2, rows.getSize());

      for (RowIterator it = rows; it.hasNext();)
      {
         Row r = it.nextRow();
         Value excerpt = r.getValue("rep:excerpt(.)");
         Value text = r.getValue("exo:text");

         if (text.getString().equals(s1))
         {

            assertEquals(string1_excerpt, excerpt.getString());
         }
         else if (text.getString().equals(s2))
         {
            assertEquals(string2_excerpt, excerpt.getString());
         }
      }
   }
	public void testExcerptWithEmptyProperty ()   throws Exception
    {
        Node node4 = testRoot.addNode("Node4", "exo:article");
        node4.setProperty("exo:title", "");
        node4.setProperty("exo:text", s1);

        testSession.save();

        QueryManager queryManager = testSession.getWorkspace().getQueryManager();
        Query query =
                queryManager.createQuery("select  excerpt(.) from exo:article where "
                         + "contains(., 'excerpt') ORDER BY exo:title", Query.SQL);
        QueryResult result = query.execute();
        RowIterator rows = result.getRows();
        for (RowIterator it = rows; it.hasNext();)
        {
            Row r = it.nextRow();
            Value excerpt = r.getValue("rep:excerpt(exo:title)");
            assertEquals(string3_excerpt, excerpt.getString());
        }
        node4.remove();
        testSession.save();
    }

   public void testExcerptWithRules() throws Exception
   {
      Node node5 = testRoot.addNode("mynode", "exo:JCR_2394_1");
      node5.addMixin("exo:sortable");
      node5.setProperty("exo:name", "myword");
      node5.setProperty("exo:title", "mydoc");
      Node resourceNode = node5.addNode("exo:content", "exo:JCR_2394_2");
      resourceNode.setProperty("exo:summary", "text");
      resourceNode.setProperty("exo:data", s3);
      testSession.save();

      String excerpt = getExcerpt("Fuzzy");
      assertNotNull(excerpt);
      assertTrue(excerpt.contains("<strong>Fuzzy</strong>"));
      node5.remove();
      testSession.save();
   }

   public void testExcerptWithRules2() throws Exception
   {
      Node node5 = testRoot.addNode("mynode", "exo:JCR_2394_1");
      node5.addMixin("exo:sortable");
      node5.setProperty("exo:name", "myword");
      node5.setProperty("exo:title", "mydoc");
      Node resourceNode = node5.addNode("exo:content", "exo:JCR_2394_2");
      resourceNode.setProperty("exo:summary", "text");
      resourceNode.setProperty("exo:data", "bla bla bla bla bla bla");
      testSession.save();

      String excerpt = getExcerpt("myword");
      assertNotNull(excerpt);
      assertFalse(excerpt.contains("<strong>"));

      excerpt = getExcerpt("text");
      assertNotNull(excerpt);
      assertTrue(excerpt.contains("<strong>text</strong>"));
      node5.remove();
      testSession.save();
   }

   private String getExcerpt(String term) throws RepositoryException
   {
      QueryManager queryManager = testSession.getWorkspace().getQueryManager();
      Query query =
         queryManager.createQuery("select  rep:excerpt() from exo:JCR_2394_1 where "
            + "contains(., '"+term+"')", Query.SQL);
      QueryResult result = query.execute();
      RowIterator rows = result.getRows();

      Value v = rows.nextRow().getValue("rep:excerpt(.)");
      if (v != null)
      {
         return v.getString();
      }
      else
      {
         return null;
      }
   }

}
