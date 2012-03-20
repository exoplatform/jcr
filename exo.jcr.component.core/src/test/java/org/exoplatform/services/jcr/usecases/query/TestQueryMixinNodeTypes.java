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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SARL Author : Pham Xuan Hoa hoa.pham@exoplatform.com Jan 16, 2008
 */
public class TestQueryMixinNodeTypes extends BaseUsecasesTest
{

   public void testQueryMixinNodeTypes() throws Exception
   {
      registerNodetypes();
      Node testFolder = root.addNode("testFolder", "nt:unstructured");
      root.save();
      // add an exo:article
      Node article = testFolder.addNode("myArticle", "exo:article");
      article.setProperty("exo:title", "title");
      article.setProperty("exo:summary", "article summary");
      article.setProperty("exo:text", "article content");

      article.addMixin("exo:datetime");
      article.setProperty("exo:dateCreated", new GregorianCalendar());
      article.setProperty("exo:dateModified", new GregorianCalendar());

      article.addMixin("mix:commentable");
      article.addMixin("mix:i18n");
      article.addMixin("exo:owneable");
      article.addMixin("mix:votable");
      session.save();
      Session systemSession = repositoryService.getCurrentRepository().getSystemSession(WORKSPACE);
      Node articleNode = (Node)systemSession.getItem("/testFolder/myArticle");
      articleNode.addMixin("exo:publishingState");
      articleNode.setProperty("exo:currentState", "Validating");
      articleNode.addMixin("exo:validationRequest");
      systemSession.save();
      systemSession.logout();

      Session session1 = repositoryService.getCurrentRepository().getSystemSession(WORKSPACE);
      Node testArticle = (Node)session1.getItem("/testFolder/myArticle");
      assertTrue(testArticle.isNodeType("exo:datetime"));
      assertTrue(testArticle.isNodeType("exo:publishingState"));
      assertTrue(testArticle.getProperty("exo:currentState").getString().equals("Validating"));

      String datetimeStatement = "//element(*,exo:datetime)";
      Query query = session1.getWorkspace().getQueryManager().createQuery(datetimeStatement, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator resNodes = result.getNodes();
      int nodesFound = 0;
      while (resNodes.hasNext())
      {
         nodesFound++;
         log
            .info("The query '" + datetimeStatement + "' found node: " + resNodes.nextNode() + ", total: " + nodesFound);
      }
      assertEquals("The search should find one node. Result size = " + resNodes.getSize() + ", actual = " + nodesFound,
         1, nodesFound);

      // Please try with both statements
      // String stateStatement =
      // "//element(*,exo:publishingState)[@exo:currentState='Validating']";
      String stateStatement = "//element(*,exo:publishingState)";
      Query stateQuery = session1.getWorkspace().getQueryManager().createQuery(stateStatement, Query.XPATH);
      QueryResult result1 = stateQuery.execute();
      assertEquals(1, result1.getNodes().getSize());
   }

   private void registerNodetypes() throws Exception
   {
      registerNamespace("kfx", "http://www.exoplatform.com/jcr/kfx/1.1/");
      registerNamespace("dc", "http://purl.org/dc/elements/1.1/");

      InputStream xml =
         this.getClass().getResourceAsStream("/org/exoplatform/services/jcr/usecases/query/ext-nodetypes-config.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager()
         .registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
      InputStream xml1 =
         this.getClass().getResourceAsStream("/org/exoplatform/services/jcr/usecases/query/nodetypes-config.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager()
         .registerNodeTypes(xml1, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
      InputStream xml2 =
         this.getClass().getResourceAsStream(
            "/org/exoplatform/services/jcr/usecases/query/nodetypes-config-extended.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager()
         .registerNodeTypes(xml2, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
      InputStream xml3 =
         this.getClass().getResourceAsStream("/org/exoplatform/services/jcr/usecases/query/nodetypes-ecm.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager()
         .registerNodeTypes(xml3, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
      InputStream xml4 =
         this.getClass().getResourceAsStream(
            "/org/exoplatform/services/jcr/usecases/query/business-process-nodetypes.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager()
         .registerNodeTypes(xml4, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
   }

   public void registerNamespace(String prefix, String uri)
   {
      try
      {
         session.getWorkspace().getNamespaceRegistry().getPrefix(uri);
      }
      catch (NamespaceException e)
      {
         try
         {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
         }
         catch (NamespaceException e1)
         {
            throw new RuntimeException(e1);
         }
         catch (RepositoryException e1)
         {
            throw new RuntimeException(e1);
         }
      }
      catch (RepositoryException e)
      {
         throw new RuntimeException(e);
      }

   }

   private ByteArrayInputStream readXmlContent(String fileName)
   {

      try
      {
         InputStream is = TestQueryMixinNodeTypes.class.getResourceAsStream(fileName);
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         int r = is.available();
         byte[] bs = new byte[r];
         while (r > 0)
         {
            r = is.read(bs);
            if (r > 0)
            {
               output.write(bs, 0, r);
            }
            r = is.available();
         }
         is.close();
         return new ByteArrayInputStream(output.toByteArray());
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         return null;
      }
   }
}
