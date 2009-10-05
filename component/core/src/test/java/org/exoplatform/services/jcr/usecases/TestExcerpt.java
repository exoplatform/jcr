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
      "<div><span>the configuration parameter for this setting is:This is the "
         + "<strong>test</strong> for Excerpt query</span></div>";

   private String s2 =
      "It is a test for Excerpt query.Searching with synonyms is integrated in the jcr:contains() "
         + "function and uses the same syntax " + "like synonym searches with Google. If a search "
         + "term is prefixed with ~ also synonyms of the search term are considered. Example:";

   private String string2_excerpt =
      "<div><span>Node2 It is a <strong>test</strong> for Excerpt query."
         + "Searching with synonyms is integrated in the jcr:contains() function and "
         + "uses the same syntax like synonym ...</span></div>";

   private String s3 = "JCR supports such features as Lucene Fuzzy Searches";

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
         ntManager.registerNodeTypes(is, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         ntManager.registerNodeTypes(TestExcerpt.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config.xml"), 0);
         ntManager.registerNodeTypes(TestExcerpt.class
            .getResourceAsStream("/org/exoplatform/services/jcr/api/nodetypes/ecm/nodetypes-config-extended.xml"), 0);
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

      Node node1 = testRoot.addNode("Node1", "exo:article");
      node1.setProperty("exo:title", "Node1");
      node1.setProperty("exo:text", s1);

      Node node2 = testRoot.addNode("Node2", "exo:article");
      node2.setProperty("exo:title", "Node2");
      node2.setProperty("exo:text", s2);

      Node node3 = testRoot.addNode("Node3", "exo:article");
      node3.setProperty("exo:title", "Node3");
      node3.setProperty("exo:text", s3);

      testSession.save();

      QueryManager queryManager = testSession.getWorkspace().getQueryManager();
      Query q1 =
         queryManager.createQuery("select exo:text, excerpt(.) from exo:article where contains(., 'test')", Query.SQL);
      QueryResult result1 = q1.execute();
      for (RowIterator it = result1.getRows(); it.hasNext();)
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

      Query q2 = queryManager.createQuery("//*[jcr:contains(., 'test')]/(@exo:text|rep:excerpt(.))", Query.XPATH);
      QueryResult result2 = q2.execute();
      for (RowIterator it = result2.getRows(); it.hasNext();)
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
}
