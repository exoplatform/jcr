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
package org.exoplatform.services.jcr.usecases.common;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS 27.04.2006
 * 
 * TODO Transient reindex of same-name siblings must be visible to a whole subtree of the reindexed
 * nodes. Currently only targeted node changes the siblings index properly. After the session save
 * all sub-nodes has right paths (and indexes). The problem has place only till the session reindex
 * (result of remove) is not saved.
 * 
 * See http://jira.exoplatform.org/browse/JCR-340
 * 
 * @version $Id: RemoveSameNameSiblingTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class RemoveSameNameSiblingTest
   extends BaseUsecasesTest
{

   public void testRemoveSameNameSibling() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");
      root.save();
      // session.save() ;
      // session.refresh(false) ;

      root.getNode("u/child[3]");
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove();
      root.save();
      // session.save() ;

      log.debug("SIZE >>>" + root.getNode("u").getNodes().getSize()); // /child[2]");
      log.debug("SIZE >>>" + session.getRootNode().getNode("u").getNodes().getSize()); // /child[2]");

      assertEquals(2, subRoot.getNodes().getSize());
      try
      {
         root.getNode("u/child[3]");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
   }

   public void testRemoveSameNameSiblingReindex() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");
      root.save();

      root.getNode("u1/child[3]");
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove();
      root.save(); // reindex child[3] --> child[2]

      log.debug("SIZE >>>" + root.getNode("u1").getNodes().getSize());
      log.debug("SIZE >>>" + session.getRootNode().getNode("u1").getNodes().getSize());

      assertEquals(2, subRoot.getNodes().getSize());
      try
      {
         root.getNode("u1/child[2]"); // 
      }
      catch (PathNotFoundException e)
      {
         fail("A node u/child[2] must exists");
      }
   }

   public void testRemoveSameNameSiblingReindexGetChilds() throws Exception
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");

      Node n3_n1n2 = n3.addNode("n1").addNode("n2");
      n3_n1n2.addNode("n2-1"); // /u1/child[3]/n1/n2/n2-1
      n3_n1n2.addNode("n2-2"); // /u1/child[3]/n1/n2/n2-2
      n3_n1n2.addNode("n2-3"); // /u1/child[3]/n1/n2/n2-3

      root.save();

      n3 = root.getNode("u1/child[3]");
      String n3id = ((NodeImpl) n3).getData().getIdentifier();
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove(); // reindex child[3] --> child[2]
      // root.save();

      assertEquals("Same-name siblings path must be reindexed", "/u1/child[2]/n1/n2", n3_n1n2.getPath());

      try
      {
         NodeIterator chns = n3_n1n2.getNodes();
         while (chns.hasNext())
         {
            Node chn = chns.nextNode();
            assertTrue("Node path must be reindexed ", chn.getPath().startsWith("/u1/child[2]"));
         }

         assertEquals("Ids must be same", n3id, ((NodeImpl) root.getNode("u1/child[2]")).getData().getIdentifier());

         try
         {
            root.getNode("u1/child[3]");
         }
         catch (PathNotFoundException e)
         {
            // ok
         }
      }
      catch (PathNotFoundException e)
      {
         fail("Nodes must exists but " + e);
      }
      finally
      {
         try
         {
            root.getNode("u1").remove();
            session.save();
         }
         catch (Exception e)
         {
            log.error("Erroro of remove " + e, e);
            throw new Exception(e);
         }
      }
   }

   public void testSearchByJcrPathSQL() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");

      Node n3_n1n2 = n3.addNode("n1").addNode("n2");
      n3_n1n2.addNode("n2-1"); // /u1/child[3]/n1/n2/n2-1
      n3_n1n2.addNode("n2-2"); // /u1/child[3]/n1/n2/n2-2
      n3_n1n2.addNode("n2-3"); // /u1/child[3]/n1/n2/n2-3

      root.save();

      root.getNode("u1/child[3]");
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove(); // reindex child[3] --> child[2]
      root.save();

      try
      {
         Query query =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:base where jcr:path like '/u1/child[3]/%'", Query.SQL);
         QueryResult queryResult = query.execute();
         NodeIterator iterator = queryResult.getNodes();
         while (iterator.hasNext())
         {
            fail("No nodes should exists");
         }

         query =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:base where jcr:path like '/u1/child[2]/%'", Query.SQL);
         queryResult = query.execute();
         iterator = queryResult.getNodes();
         while (iterator.hasNext())
         {
            Node n = iterator.nextNode();
            assertTrue("Node path must be reindexed ", n.getPath().startsWith("/u1/child[2]"));
         }
      }
      catch (RepositoryException e)
      {
         fail(e.getMessage());
      }
   }

   public void testSearchByJcrPathXPath() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");

      Node n3_n1n2 = n3.addNode("n1").addNode("n2");
      n3_n1n2.addNode("n2-1"); // /u1/child[3]/n1/n2/n2-1
      n3_n1n2.addNode("n2-2"); // /u1/child[3]/n1/n2/n2-2
      n3_n1n2.addNode("n2-3"); // /u1/child[3]/n1/n2/n2-3

      root.save();

      root.getNode("u1/child[3]");
      n2 = subRoot.getNode("child[2]");
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove(); // reindex child[3] --> child[2]
      root.save();

      try
      {
         Query query =
                  session.getWorkspace().getQueryManager().createQuery("/jcr:root/u1/child[3]//element(*, nt:base)",
                           Query.XPATH);
         QueryResult queryResult = query.execute();
         NodeIterator iterator = queryResult.getNodes();
         while (iterator.hasNext())
         {
            fail("No nodes should exists");
         }

         query =
                  session.getWorkspace().getQueryManager().createQuery("/jcr:root/u1/child[2]//element(*, nt:base)",
                           Query.XPATH);
         queryResult = query.execute();
         iterator = queryResult.getNodes();
         while (iterator.hasNext())
         {
            Node n = iterator.nextNode();
            assertTrue("Node path must be reindexed ", n.getPath().startsWith("/u1/child[2]"));
         }
      }
      catch (RepositoryException e)
      {
         fail(e.getMessage());
      }
   }

   public void testDeleteRollback() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");

      Node n3_n1n2 = n3.addNode("n1").addNode("n2");
      n3_n1n2.addNode("n2-1"); // /u1/child[3]/n1/n2/n2-1
      n3_n1n2.addNode("n2-2"); // /u1/child[3]/n1/n2/n2-2
      n3_n1n2.addNode("n2-3"); // /u1/child[3]/n1/n2/n2-3

      root.save();

      root.getNode("u1/child[3]");
      n2 = subRoot.getNode("child[2]");
      String n2id = ((NodeImpl) n2).getData().getIdentifier();
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove(); // reindex child[3] --> child[2]

      try
      {
         root.refresh(false);

         assertEquals("Ids must be same", n2id, ((NodeImpl) root.getNode("u1/child[2]")).getData().getIdentifier());

         root.getNode("u1/child[3]");

      }
      catch (RepositoryException e)
      {
         fail(e.getMessage());
      }
   }

   public void testDeleteRofresh() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("u1");
      Node n1 = subRoot.addNode("child", "nt:unstructured");
      Node n2 = subRoot.addNode("child", "nt:unstructured");
      Node n3 = subRoot.addNode("child", "nt:unstructured");

      Node n3_n1n2 = n3.addNode("n1").addNode("n2");
      n3_n1n2.addNode("n2-1"); // /u1/child[3]/n1/n2/n2-1
      n3_n1n2.addNode("n2-2"); // /u1/child[3]/n1/n2/n2-2
      n3_n1n2.addNode("n2-3"); // /u1/child[3]/n1/n2/n2-3

      root.save();

      n3 = root.getNode("u1/child[3]");
      n2 = subRoot.getNode("child[2]");
      String n3id = ((NodeImpl) n3).getData().getIdentifier();
      log.debug(">>>> SAME NAME start " + n2.getPath() + " " + n2.getIndex());
      n2.remove(); // reindex child[3] --> child[2]

      try
      {
         root.refresh(true);

         assertEquals("Ids must be same", n3id, ((NodeImpl) root.getNode("u1/child[2]")).getData().getIdentifier());

         try
         {
            root.getNode("u1/child[3]");
         }
         catch (PathNotFoundException e)
         {
            // ok
         }

      }
      catch (RepositoryException e)
      {
         fail(e.getMessage());
      }
   }

   public void testRemoveSameNameSiblings() throws Exception
   {

      Node testRoot = root.addNode("snsRemoveTest");
      session.save();

      try
      {

         Node node1 = testRoot.addNode("_node");
         node1.setProperty("prop", "_data1");
         Node node2 = testRoot.addNode("_node");
         node2.setProperty("prop", "_data2");
         Node node3 = node2.addNode("node3");
         testRoot.save();

         try
         {
            assertEquals("/snsRemoveTest/_node[2]/node3", node2.getNode("node3").getPath());
            node1.remove(); // /snsRemoveTest/_node[2] -> /snsRemoveTest/_node[1]

            // check
            String n2p = node2.getProperty("prop").getString();
            assertEquals("A property must be same ", "_data2", n2p);

            // TODO there is a problem, we can't see deep subtree of reindexed same-name-siblings now.
            // after save it will be ok.
            // See http://jira.exoplatform.org/browse/JCR-340
            assertEquals("/snsRemoveTest/_node/node3", node2.getNode("node3").getPath());

         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
            fail("A property must exists on the node /snsRemoveTest/_node[1] " + e);
         }
      }
      finally
      {
         testRoot.remove();
         session.save();
      }
   }
}
