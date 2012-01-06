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
package org.exoplatform.services.jcr.api.reading;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;

import java.security.AccessControlException;
import java.util.Calendar;
import java.util.HashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestNode.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestNode extends JcrAPIBaseTest
{

   private Node testRoot;

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node file = root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode = file.getNode("jcr:content");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();
   }

   public void tearDown() throws Exception
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      node.remove();
      session.save();

      super.tearDown();
   }

   public void testGetNode() throws Exception
   {

      Node root = session.getRootNode();

      try
      {
         root.getNode("/childNode/childNode2");
         fail("exception should have been thrown - not rel path");
      }
      catch (RepositoryException e)
      {
      }

      Node node = root.getNode("childNode/childNode2");
      assertNotNull(node);

      assertEquals("nt:file", node.getPrimaryNodeType().getName());
      Property property = node.getNode("jcr:content").getProperty("jcr:data");
      property.setValue(new StringValue("this is the NEW content"));

      node = root.getNode("childNode");
      node.addNode("childNode3", "nt:file");

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      try
      {
         Node n = root.getNode("childNode/childNode3");
         fail("exception should have been thrown " + n);
      }
      catch (RepositoryException e)
      {
      }

      property = root.getNode("childNode/childNode2/jcr:content").getProperty("jcr:data");

      assertEquals("this is the content", property.getString());
      Value val = new BinaryValue("this is the NEW content");
      node = root.getNode("childNode/childNode2/jcr:content");
      node.setProperty("jcr:data", val);
      // property.setValue(val);

      node = root.getNode("childNode");
      session.save();
      root = repository.login(credentials, WORKSPACE).getRootNode();
      // System.out.println("------------------");
      property = root.getNode("childNode/childNode2/jcr:content").getProperty("jcr:data");

      assertEquals("this is the NEW content", property.getString());

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      node = root.getNode("childNode");
      assertEquals(node.toString(), root.getNode("childNode").toString());

      // not allowed!
      // root.getNode("childNode/childNode2/jcr:content").setProperty("myapp:temp",
      // new
      // StringValue("Temp"));

      Session session2 = repository.login(credentials, WORKSPACE);
      Node root2 = session2.getRootNode();
      Node node2 = root2.getNode("childNode/childNode2/jcr:content");
      node2.setProperty("jcr:data", new BinaryValue("Temp"));
      session2.save();

      session.refresh(false);

      root = session.getRootNode();
      node = root.getNode("childNode/childNode2/jcr:content");
      assertNotNull(node);
      assertNotNull(node.getProperty("jcr:data"));
      assertEquals("Temp", node.getProperty("jcr:data").getString());
      try
      {
         node.getProperty("myapp:temp");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

   }

   public void testGetSomeSiblingNode() throws RepositoryException
   {
      root = session.getRootNode();
      Node subRoot = root.addNode("subRoot", "nt:unstructured");
      Node child1 = subRoot.addNode("child", "nt:unstructured");
      child1.setProperty("prop1", "prop1");
      Node child2 = subRoot.addNode("child", "nt:unstructured");
      child2.setProperty("prop2", "prop2");
      Node child3 = subRoot.addNode("child", "nt:unstructured");
      assertEquals(1, child1.getIndex());
      assertTrue(child1.hasProperty("prop1"));
      assertEquals(2, child2.getIndex());
      assertTrue(child2.hasProperty("prop2"));
      assertEquals(3, child3.getIndex());

      root.save();
      // System.out.println(">>"+session.getContainer());
      subRoot = root.getNode("subRoot");
      child1 = subRoot.getNode("child");
      assertEquals(1, child1.getIndex());
      assertTrue(child1.hasProperty("prop1"));
      NodeIterator children = subRoot.getNodes();
      assertEquals(3, (int)children.getSize());
      child1 = (Node)children.next();
      assertEquals(1, child1.getIndex());
      assertTrue(child1.hasProperty("prop1"));
      child2 = (Node)children.next();
      assertEquals(2, child2.getIndex());
      assertTrue(child2.hasProperty("prop2"));

      // read first same name sibling
      child1 = (Node)session.getItem("/subRoot/child");
      assertEquals("Not returned first item", 1, child1.getIndex());

      subRoot.remove();
      root.save();
      // subRoot.save(); ipossible to call save() on removed node
   }

   public void testGetNodes() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      log.debug("ChildNode before refresh " + node);

      node.addNode("childNode4", "nt:folder");

      NodeIterator nodeIterator = node.getNodes();
      while (nodeIterator.hasNext())
      {
         node = (Node)nodeIterator.next();
         assertNotNull(node.getSession());
         if (!("childNode4".equals(node.getName()) || "childNode2".equals(node.getName())))
            fail("returned non expected nodes" + node.getName() + " " + node);
      }

      Session session2 = repository.login(credentials, WORKSPACE);
      Node root2 = session2.getRootNode();
      Node node2 = root2.getNode("childNode");
      Node node5 = node2.addNode("childNode5", "nt:folder");
      session2.save();

      session.refresh(false);

      node = root.getNode("childNode");
      // log.debug("ChildNode after refresh "+node+" "+((NodeImpl)node).isChildNodesInitialized());

      nodeIterator = node.getNodes();

      while (nodeIterator.hasNext())
      {
         node = (Node)nodeIterator.next();
         if (!("childNode5".equals(node.getName()) || "childNode2".equals(node.getName())))
            fail("returned non expected nodes " + node.getName() + "  " + node);
      }

      node5.remove();
      session2.save();
   }

   public void testGetNodesWithNamePattern() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      node.addNode("childNode4", "nt:folder");
      node.addNode("otherNode", "nt:folder");
      node.addNode("lastNode", "nt:folder");

      Node result = (Node)node.getNodes("lastNode").next();
      assertEquals("lastNode", result.getName());

      NodeIterator iterator = node.getNodes("otherNode | lastNode");
      if (!iterator.hasNext())
         fail("nodes should have been found");
      while (iterator.hasNext())
      {
         Node nodeTmp = iterator.nextNode();
         if (!("otherNode".equals(nodeTmp.getName()) || "lastNode".equals(nodeTmp.getName())))
            fail("returned non expected nodes");
      }

      iterator = node.getNodes("childNode*");
      if (!iterator.hasNext())
         fail("nodes should have been found");
      while (iterator.hasNext())
      {
         Node nodeTmp = iterator.nextNode();
         if (!("childNode2".equals(nodeTmp.getName()) || "childNode4".equals(nodeTmp.getName())))
            fail("returned non expected nodes");
      }

      Session session2 = repository.login(credentials, WORKSPACE);
      Node root2 = session2.getRootNode();
      Node node2 = root2.getNode("childNode");
      node2.addNode("childNode5", "nt:folder");
      session2.save();

      session.refresh(false);
      node = root.getNode("childNode");
      iterator = node.getNodes("childNode*");
      if (!iterator.hasNext())
         fail("nodes should have been found");
      while (iterator.hasNext())
      {
         Node nodeTmp = iterator.nextNode();
         if (!("childNode2".equals(nodeTmp.getName()) || "childNode5".equals(nodeTmp.getName())))
            fail("returned non expected nodes");
      }
   }

   public void testGetProperty() throws RepositoryException
   {

      final String valueNew = "this is the NEW value";

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode/childNode2/jcr:content");
      Property property = node.getProperty("jcr:data");
      assertEquals("this is the content", property.getString());

      Session session2 = repository.login(credentials, WORKSPACE);
      Node root2 = session2.getRootNode();
      Node node2 = root2.getNode("childNode/childNode2/jcr:content");
      // log.debug("Set prop");
      node2.getProperty("jcr:data").setValue(valueFactory.createValue(valueNew.toString(), PropertyType.BINARY));
      // node2.setProperty("jcr:data",
      // valueFactory.createValue("this is the NEW value",
      // PropertyType.BINARY));
      session2.save();
      // log.debug("Set prop end");

      assertEquals(valueNew.toString(), ((Property)session2.getItem("/childNode/childNode2/jcr:content/jcr:data"))
         .getString());

      assertEquals("this is the NEW value", root2.getNode("childNode/childNode2/jcr:content").getProperty("jcr:data")
         .getString());

      Session session3 = repository.login(credentials, WORKSPACE);
      Node root3 = session3.getRootNode();
      Node node3 = root3.getNode("childNode/childNode2/jcr:content");
      assertEquals(valueNew.toString(), ((Property)session3.getItem("/childNode/childNode2/jcr:content/jcr:data"))
         .getString());
      assertEquals(valueNew.toString(), node3.getProperty("jcr:data").getString());

      node.refresh(false);
      // session = repository.login(credentials, WORKSPACE);

      property = root.getNode("childNode/childNode2/jcr:content").getProperty("jcr:data");
      assertEquals("/childNode/childNode2/jcr:content/jcr:data", property.getPath());
      assertEquals(valueNew.toString(), property.getString());
   }

   public void testGetProperties() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");

      PropertyIterator iterator = node.getProperties();
      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("jcr:primaryType".equals(property.getName()) || "jcr:created".equals(property.getName()) || "jcr:lastModified"
            .equals(property.getName())))
            fail("returned non expected nodes");
      }

      Session session2 = repository.login(credentials, WORKSPACE);
      Node root2 = session2.getRootNode();
      Node node2 = root2.getNode("childNode/childNode2/jcr:content");
      node2.setProperty("jcr:data", session.getValueFactory().createValue("hehe", PropertyType.BINARY));
      session2.save();

      session.refresh(false);
      node = root.getNode("childNode/childNode2/jcr:content");
      iterator = node.getProperties();

      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         log.debug("PROP---" + property);
      }
   }

   public void testGetPropertiesWithNamePattern() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      // Node node = root.getNode("/childNode/childNode2/jcr:content");

      Node node = root.addNode("testNode", "nt:unstructured");

      node.setProperty("property1", "prop1Value");
      node.setProperty("property2", "prop2Value");

      PropertyIterator iterator = node.getProperties("property1 | property2");

      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("property1".equals(property.getName()) || "property2".equals(property.getName())))
            fail("returned non expected properties");
      }

      iterator = node.getProperties("property1 | jcr:*");

      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("property1".equals(property.getName()) || "jcr:primaryType".equals(property.getName())))
            fail("returned non expected properties");
      }

   }

   public void testGetPropertiesWithNamePatternStoredData() throws RepositoryException
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      // Node node = root.getNode("/childNode/childNode2/jcr:content");

      Node node = root.addNode("testNode", "nt:unstructured");

      node.setProperty("property1", "prop1Value");
      node.setProperty("property2", "prop2Value");
      root.save();

      PropertyIterator iterator = node.getProperties("property1 | property2");

      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("property1".equals(property.getName()) || "property2".equals(property.getName())))
            fail("returned non expected properties");
      }

      iterator = node.getProperties("property1 | jcr:*");

      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("property1".equals(property.getName()) || "jcr:primaryType".equals(property.getName())))
            fail("returned non expected properties");
      }

      node.setProperty("proper_ty", "prop_value");
      node.setProperty("properAty", "propAvalue");
      root.save();

      iterator = node.getProperties("proper_t%");
      while (iterator.hasNext())
      {
         Property property = iterator.nextProperty();
         if (!("proper_ty".equals(property.getName())))
            fail("returned non expected properties");
      }
   }

   public void testGetPrimaryItem() throws RepositoryException
   {
      Node root = session.getRootNode();
      try
      {
         root.getPrimaryItem();
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
         assertTrue(e instanceof ItemNotFoundException);
      }

      Node node = root.getNode("childNode/childNode2");
      Item item = node.getPrimaryItem();
      assertNotNull(item);
      assertEquals("jcr:content", item.getName());
   }

   public void testGetUUID() throws RepositoryException
   {
      Node root = session.getRootNode();
      try
      {
         root.getUUID();
         fail("exception should have been thrown");
      }
      catch (UnsupportedRepositoryOperationException e)
      {
      }
      Node node = root.getNode("childNode/childNode2/jcr:content");
      assertTrue(session.itemExists("/childNode/childNode2/jcr:content/jcr:uuid"));
      assertNotNull(node.getUUID());
   }

   public void testGetDefinition() throws RepositoryException
   {
      Node root = session.getRootNode();
      assertNotNull(root.getDefinition());
      assertEquals("*", root.getNode("childNode").getDefinition().getName());
      assertEquals("jcr:content", root.getNode("childNode").getNode("childNode2").getNode("jcr:content")
         .getDefinition().getName());
   }

   public void testHasNode() throws RepositoryException
   {
      Node root = session.getRootNode();
      assertFalse(root.hasNode("dummyNode"));
      assertTrue(root.hasNode("childNode"));
      // root.getNode("childNode").remove();
      // assertFalse(root.hasNode(""));

   }

   public void testHasNodes() throws RepositoryException
   {
      Node root = session.getRootNode();
      // System.out.println("Node>>>"+session.getItem("/childNode"));
      // System.out.println("Node>>>"+root.getNode("childNode"));
      // System.out.println("Node>>>"+root.getNodes().next());
      assertTrue(root.hasNodes());
      // Node node = root.getNode("/childNode/childNode2/jcr:content");
      Node node = root.addNode("tempNode", "nt:unstructured");
      node = node.addNode("tempNode1", "nt:unstructured");

      assertFalse(node.hasNodes());
   }

   public void testHasProperty() throws RepositoryException
   {
      Node root = session.getRootNode();
      assertFalse(root.hasProperty("dummyProperty"));
      assertTrue(root.getNode("childNode").hasProperty("jcr:created"));
   }

   public void testHasProperties() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      assertTrue(node.hasProperties());
   }

   public void testGetNodesWithNamePatternAndSameNameSibs() throws RepositoryException
   {

      // The standard method for retrieving a set of such nodes is
      // Node.getNodes(String namePattern) which returns an iterator
      // over all the child nodes of the calling node that have the specified
      // pattern (by making namePattern just a name, without wildcards,
      // we can get all the nodes with that exact name, see section

      Node root = session.getRootNode();
      Node node = root.addNode("snTestNode");
      node.addNode("sn");
      node.addNode("sn");
      node.addNode("sn");

      NodeIterator i = node.getNodes("sn");
      assertEquals(3l, i.getSize());
   }

   public void testAddMixinWhenNodeIsProtected() throws NoSuchNodeTypeException, VersionException,
      LockException, ItemExistsException, PathNotFoundException, RepositoryException
   {                 
      try
      {
         root.addNode("someNode", "exo:myTypeJCR1703").getNode("exo:myChildNode").addMixin("mix:lockable");
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testCannotAddNodeWithThisRelPath()
   {
      try
      {
         root.addNode(".");
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testCanAddMixinWhenNodeIsProtected() throws NoSuchNodeTypeException, PathNotFoundException,
      ItemExistsException, LockException, VersionException, ConstraintViolationException, RepositoryException
   {
      Node node = root.addNode("someNode", "exo:myTypeJCR1703").getNode("exo:myChildNode");
      assertFalse(node.canAddMixin("mix:lockable"));
   }

   public void testCannotDoCheckinWhenMergeFailedIsSet() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node testNode = root.addNode("testNode");
      testNode.setProperty("jcr:mergeFailed", "");
      testNode.addMixin("mix:versionable");
      session.save();

      try
      {
         testNode.checkin();
         fail();
      }
      catch (VersionException e)
      {
      }
   }

   public void testCannotDoCheckinWhenNodeIsLocked() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      Node testNode = root.addNode("test");
      testNode.addMixin("mix:lockable");
      testNode.addMixin("mix:versionable");
      session.save();
      testNode.lock(true, true);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());

      try
      {
         session2.getRootNode().getNode("test").checkin();
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         testNode.unlock();
      }
   }

   public void testCannotClearACLForNotExoPrivilegeableNode() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      try
      {
         ((NodeImpl)root.addNode("testNode")).clearACL();
         fail();
      }
      catch (AccessControlException e)
      {
      }
   }

   public void testEquals() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      assertFalse(root.addNode("testNode").equals(new Object()));
   }

   public void testCannotGetBaseVersionForNotVersionableNode() throws Exception
   {
      try
      {
         root.addNode("testNode").getBaseVersion();
         fail();
      }
      catch (UnsupportedRepositoryOperationException e)
      {
      }
   }

   public void testLockWithIsSessionScopedWhenUnsavedChanges() throws UnsupportedRepositoryOperationException,
      LockException, AccessDeniedException, ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      testNode.addMixin("mix:lockable");

      try
      {
         testNode.lock(true, false);
         fail();
      }
      catch (InvalidItemStateException e)
      {
      }
   }

   public void testLockWithTimeOutScopedWhenUnsavedChanges() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      testNode.addMixin("mix:lockable");

      try
      {
         testNode.lock(true, 10L);
         fail();
      }
      catch (InvalidItemStateException e)
      {
      }
   }

   public void testLockWithTimeOutScopedWhenLockException() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      testNode.addMixin("mix:lockable");
      session.save();

      testNode.lock(true, false);

      try
      {
         testNode.lock(true, 10L);
         fail();
      }
      catch (LockException e)
      {
      }
   }

   public void testCannotRemovePermissionForNotExoPrivileageableNode() throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      NodeImpl testChildNode = (NodeImpl)testNode.addNode("testChildNode");

      try
      {
         testNode.removePermission(testChildNode.getIdentifier());
         fail();
      }
      catch (AccessControlException e)
      {
      }

      try
      {
         testNode.removePermission(testChildNode.getIdentifier(), "jonh");
         fail();
      }
      catch (AccessControlException e)
      {
      }
   }

   public void testSetPermission() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");

      try
      {
         testNode.setPermission("john", PermissionType.ALL);
         fail();
      }
      catch (AccessControlException e)
      {
      }

      testNode.addMixin("exo:privilegeable");
      session.save();

      try
      {
         testNode.setPermission(null, PermissionType.ALL);
         fail();
      }
      catch (RepositoryException e)
      {
      }

      try
      {
         testNode.setPermission("john", null);
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testSetPermissions() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      HashMap<String, String[]> permissions = new HashMap<String, String[]>();

      try
      {
         testNode.setPermissions(permissions);
         fail();
      }
      catch (AccessControlException e)
      {
      }

      testNode.addMixin("exo:privilegeable");
      session.save();

      permissions.put(null, PermissionType.ALL);

      try
      {
         testNode.setPermissions(permissions);
         fail();
      }
      catch (RepositoryException e)
      {
      }

      permissions.remove(null);
      permissions.put("jonh", null);

      try
      {
         testNode.setPermissions(permissions);
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testUnlockWhenUnsavedChanges() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      Node someNode = root.addNode("someNode");
      someNode.addMixin("mix:lockable");
      session.save();

      someNode.lock(true, false);
      someNode.addNode("temp");

      try
      {
         someNode.unlock();
         fail();
      }
      catch (InvalidItemStateException e)
      {
      }
   }

   public void testUpdateNodeIsLocked() throws NoSuchWorkspaceException, AccessDeniedException,
      InvalidItemStateException, PathNotFoundException, ItemExistsException, NoSuchNodeTypeException, VersionException,
      ConstraintViolationException, RepositoryException
   {
      Node someNode = root.addNode("someNode");
      someNode.addMixin("mix:lockable");
      session.save();

      someNode.lock(true, false);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());


      try
      {
         session2.getRootNode().getNode("someNode").update("/");
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         someNode.unlock();
      }
   }

   public void testValidateChildNodeWhenNodeIsProtected() throws PathNotFoundException, ItemExistsException,
      NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("someNode", "exo:myTypeJCR1703").getNode("exo:myChildNode");

      try
      {
         ((NodeImpl)root.getNode("someNode")).validateChildNode(testNode.getInternalName(),
            ((NodeTypeImpl)testNode.getPrimaryNodeType()).getQName());
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testVersionHistoryWhenNodeHaventVersionHistory() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("someNode");

      try
      {
         testNode.versionHistory(false);
         fail();
      }
      catch (UnsupportedRepositoryOperationException e)
      {
      }
   }

   public void testRemoveMixinWhenRemovedLockableMixin() throws ItemExistsException, PathNotFoundException,
      VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      Node someNode = root.addNode("someNode");
      someNode.addMixin("mix:lockable");
      session.save();

      someNode.lock(true, false);

      Session session2 =
         repository.login(new CredentialsImpl("admin", "admin".toCharArray()), session.getWorkspace().getName());

      try
      {
         session2.getRootNode().getNode("someNode").removeMixin("mix:lockable");
         fail();
      }
      catch (LockException e)
      {
      }
      finally
      {
         session2.logout();
         someNode.unlock();
      }
   }
}
