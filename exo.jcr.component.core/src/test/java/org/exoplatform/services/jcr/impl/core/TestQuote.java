/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestQuote extends JcrImplBaseTest
{
   private static final String NODE_NAME = "My Node \"with a '\"";
   private static final String PROPERTY_NAME = "My Property \"with a '\"";
   
   private Node testRootNode;
   
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testRootNode = session.getRootNode().addNode("TestQuote");
      session.save();
   }



   @Override
   protected void tearDown() throws Exception
   {
      if (testRootNode != null)
      {
         testRootNode.remove();
         session.save();
      }
      super.tearDown();
   }

   public void testCRUD() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      testRootNode.save();
      Session session2 = repository.login(credentials, "ws");
      assertTrue(session2.itemExists("/TestQuote/" + NODE_NAME));
      assertTrue(session2.itemExists("/TestQuote/" + NODE_NAME + "/" + PROPERTY_NAME));
      Node n = (Node) session2.getItem("/TestQuote/" + NODE_NAME);
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertEquals("my value", n.getProperty(PROPERTY_NAME).getString());
      Node subNode = n.getParent().addNode(NODE_NAME + "/sub-" + NODE_NAME);
      subNode.addMixin("exo:owneable");
      subNode.setProperty(PROPERTY_NAME, "my other value");
      subNode.setProperty(PROPERTY_NAME + "2", "my other value 2");
      n.getParent().addNode(NODE_NAME + "/sub-" + NODE_NAME + "2");
      n.save();
      assertTrue(node.hasNode("sub-" + NODE_NAME));
      Node sn = node.getNode("sub-" + NODE_NAME);
      assertTrue(sn.hasProperty(PROPERTY_NAME));
      assertEquals("my other value", sn.getProperty(PROPERTY_NAME).getString());
      PropertyIterator it = sn.getProperties(PROPERTY_NAME + "|" + PROPERTY_NAME + "2|exo:owner");
      int propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }         
      }
      assertEquals(3, propertyFound);
      it = sn.getProperties();
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }         
      }
      assertEquals(3, propertyFound);
      NodeIterator nIt = node.getNodes("sub-" + NODE_NAME + "|sub-" + NODE_NAME + "2");
      int nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }         
      }
      assertEquals(2, nodeFound);
      nIt = node.getNodes();
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }         
      }
      assertEquals(2, nodeFound);
      
      // Clear cache
      ManageableRepository repository = repositoryService.getDefaultRepository();
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");

      CacheableWorkspaceDataManager dm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      WorkspaceStorageCache cache = dm.getCache();
      if (cache.isEnabled() && cache instanceof Backupable)
      {
         ((Backupable)cache).clean();
      }
      
      it = sn.getProperties(PROPERTY_NAME + "|" + PROPERTY_NAME + "2|exo:owner");
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }         
      }
      assertEquals(3, propertyFound);
      it = sn.getProperties();
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }         
      }
      assertEquals(3, propertyFound);
      nIt = node.getNodes("sub-" + NODE_NAME + "|sub-" + NODE_NAME + "2");
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }         
      }
      assertEquals(2, nodeFound);
      nIt = node.getNodes();
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }         
      }
      assertEquals(2, nodeFound);
      
      Node node2 = testRootNode.addNode(NODE_NAME);
      node2.addMixin("mix:referenceable");
      node2.setProperty(PROPERTY_NAME, "my value 3");
      testRootNode.save();
      String uuid = node2.getUUID();
      Node n2 = session2.getNodeByUUID(uuid);
      assertTrue(n2.hasProperty(PROPERTY_NAME));
      assertEquals("my value 3", n2.getProperty(PROPERTY_NAME).getString());
      node2.setProperty(PROPERTY_NAME, "my value 4");
      testRootNode.save();
      assertEquals("my value 4", n2.getProperty(PROPERTY_NAME).getString());
      node2.setProperty(PROPERTY_NAME, (String)null);
      testRootNode.save();
      assertFalse(n2.hasProperty(PROPERTY_NAME));
      node2.remove();
      testRootNode.save();
      try
      {
         session2.getNodeByUUID(uuid);
         fail("ItemNotFoundException was expected");
      }
      catch (ItemNotFoundException e)
      {
         // expected exception
      }
      session2.logout();
   }
   
   public void testLock() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      testRootNode.save();
      try
      {
         node.lock(false, true);
         fail("a LockException was expected");
      }
      catch (LockException e)
      {
         // expected exception
      }
      node.addMixin("mix:lockable");
      testRootNode.save();
      node.lock(false, true);
      assertTrue(node.isLocked());
      Session session2 = repository.login(credentials, "ws");
      Node n = (Node) session2.getItem("/TestQuote/" + NODE_NAME);
      assertTrue(n.isLocked());
      try
      {
         n.lock(false, true);
         fail("a LockException was expected");
      }
      catch (LockException e)
      {
         // expected exception
      }
      node.unlock();
      assertFalse(node.isLocked());
      n.lock(false, true);
      assertTrue(node.isLocked());
      n.unlock();
      assertFalse(node.isLocked());
      session2.logout();
   }
   
   public void testVersioning() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      node.addNode("sub-" + NODE_NAME);
      node.addMixin("mix:versionable");
      testRootNode.save();
      assertTrue(node.isCheckedOut());
      node.checkin();
      assertFalse(node.isCheckedOut());
      node.checkout();
      node.setProperty(PROPERTY_NAME, "my value 2");
      testRootNode.save();
      node.checkin();
      assertFalse(node.isCheckedOut());
      node.checkout();
      node.setProperty(PROPERTY_NAME, "my value 3");
      testRootNode.save();
      node.checkin();
      Session session2 = repository.login(credentials, "ws");
      Node n = (Node) session2.getItem("/TestQuote/" + NODE_NAME);
      assertFalse(n.isCheckedOut());
      node.checkout();
      assertTrue(n.isCheckedOut());
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertTrue(n.hasNode("sub-" + NODE_NAME));
      assertEquals("my value 3", n.getProperty(PROPERTY_NAME).getString());
      
      Version baseVersion = node.getBaseVersion();
      VersionHistory vh = node.getVersionHistory();
      Version v = vh.getRootVersion();
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value 2", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value 3", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      node.restore(vh.getRootVersion().getSuccessors()[0], true);
      
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertTrue(n.hasNode("sub-" + NODE_NAME));
      assertEquals("my value", n.getProperty(PROPERTY_NAME).getString());
      
      session2.logout();
   }
}
