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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.Calendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestAddNode.java 12651 2008-04-02 15:26:26Z ksm $
 */
public class TestAddNode extends JcrAPIBaseTest
{

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node file = root.addNode("TestNodesTree", "nt:folder").addNode("childNode2", "nt:file");
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
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("TestNodesTree");
      node.remove();
      root.save();
      // node.save();ipossible to call save() on removed node

      super.tearDown();
   }

   /**
    * Test case-sensitive names
    */
   public void testAddNodeCSNames() throws RepositoryException
   {
      try
      {
         Node localRoot = root.addNode("testCI", "nt:folder");
         root.save();
         localRoot.addNode("test", "nt:folder");
         localRoot.addNode("Test", "nt:folder");
         root.save();
      }
      catch (ItemExistsException e)
      {
         fail("Repository must be case-sensitive!");
      }
   }

   public void testAddNode() throws RepositoryException
   {
      Node root = session.getRootNode();

      // If relPath implies intermediary nodes that do not exist,
      // then a PathNotFoundException is thrown.
      try
      {
         root.addNode("notExisting/dummy");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      // If an item already exists at relPath, and same-name
      // siblings are not permitted, then an ItemExistsException
      // is thrown immediately (not on save).
      try
      {
         root.getNode("TestNodesTree").addNode("childNode2", "nt:file");
         fail("exception should have been thrown");
      }
      catch (ItemExistsException e)
      {
      }

      try
      {
         root.addNode("TestNodesTree/childNode2/jcr:primaryType/test");
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      // undefined nodetype
      Node node = root.getNode("TestNodesTree/childNode2");
      try
      {
         node.addNode("notJcrContentNode");
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }


      Node testNode = root.addNode("testNode", "exo:EXOJCR-1869");
      try
      {
         testNode.addNode("node1", "exo:EXOJCR-1869-2");
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      testNode.addMixin("mix:EXOJCR-1869-1");
      try
      {
         testNode.addNode("A1", "exo:EXOJCR-1869-3");
      }
      catch (ConstraintViolationException e)
      {
         fail("exception should not thrown");
      }

      try
      {
         testNode.addNode("A2", "exo:EXOJCR-1869-3");
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      testNode.addMixin("mix:EXOJCR-1869-2");
      try
      {
         testNode.addNode("A2", "exo:EXOJCR-1869-4");
      }
      catch (ConstraintViolationException e)
      {
         fail("exception should not thrown");
      }

      try
      {
         testNode.addNode("A1", "exo:EXOJCR-1869-4");
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }
   }

   public void testAddAndSaveNodeWithNodeType() throws RepositoryException
   {

      Node root = session.getRootNode();
      Node node = root.getNode("TestNodesTree/childNode2");
      try
      {
         node.addNode("dummy", "nt:base");
         session.save();
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      root = session.getRootNode();
      node = root.getNode("TestNodesTree");
      try
      {
         node.addNode("dummy1", "nt:dymyNT");
         fail("exception should have been thrown");
      }
      catch (NoSuchNodeTypeException e)
      {
      }

   }

   public void testIfAddedNodeAppearsInGetNodesImmediatelly() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node root1 = root.addNode("root1", "nt:unstructured");
      assertEquals(0, root1.getNodes().getSize());
      root1.addNode("node1", "nt:unstructured");
      assertEquals(1, root1.getNodes().getSize());
      assertTrue(root1.hasNode("node1"));
      assertNotNull(root1.getNode("node1"));
   }

   public void testAddingSameNameSibs() throws RepositoryException
   {

      // the same node already exists but same name sibs
      Node subRoot = root.addNode("subRoot", "nt:unstructured");
      subRoot = root.addNode("subRoot", "nt:unstructured");
      Node node1 = subRoot.addNode("TestNodesTree", "nt:unstructured");
      Node node2 = subRoot.addNode("TestNodesTree", "nt:unstructured");
      assertNotNull(node1);
      NodeIterator nodes = subRoot.getNodes();
      assertEquals(2, (int)nodes.getSize());
      try
      {
         subRoot.addNode("TestNodesTree[3]", "nt:unstructured");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

   }

   public void testInvalidItemStateException() throws RepositoryException
   {
      Node n = session.getRootNode().addNode("sameNode", "nt:base");

      Session session2 = repository.login(credentials, "ws");
      Node n2 = session2.getRootNode().addNode("sameNode", "nt:base");
      session.save();

      try
      {
         session2.save();
         fail("InvalidItemStateException should have been thrown");
      }
      catch (ItemExistsException e)
      {
      }
   }

   public void testAddingNodeToDiffWorkspaces() throws RepositoryException
   {

      String[] wsNames = repository.getWorkspaceNames();
      if (wsNames.length < 2)
      {
         fail("Too few number of ws for test should be > 1");
      }
      log.debug(">>>>>>>> " + wsNames.length);
      for (int i = 0; i < wsNames.length; i++)
      {
         log.debug(">>>>>>>> " + wsNames[i]);
      }

      // Session s1 = repository.login(credentials, wsNames[0]);
      Session s1 = repository.getSystemSession(wsNames[0]);

      s1.getRootNode().addNode("test1", "nt:unstructured");
      s1.save();
      s1 = repository.login(credentials, wsNames[0]);
      log.debug("S1 >>>>" + wsNames[0] + " : " + s1.getItem("/test1"));
      log.debug("S1 >>>>" + wsNames[0] + " : " + s1 + " " + s1.getWorkspace().getName());

      // Session s2 = repository.getSystemSession(wsNames[1]);

      // s2.getRootNode().addNode("test2", "nt:unstructured");
      // s2.save();
      // s2 = repository.login(credentials, wsNames[1]);
      // log.debug("S2 "+wsNames[1]+" : "+s2.getItem("/test2"));
      // log.debug("S2 >>>>"+wsNames[1]+" : "+s2+" "+s2.getWorkspace().getName());
   }

   public void testDeleteAndAddNode() throws RepositoryException
   {
      log.debug(">>> start getRoot");
      Node root = session.getRootNode();
      log.debug(">>> start remove");
      root.getNode("TestNodesTree").remove();
      log.debug(">>> end remove");
      root.addNode("TestNodesTree");
      log.debug(">>> end add");
      root.save();
      log.debug(">>> end save");
      log.debug(">>> " + root.getNode("TestNodesTree"));
   }

   public void testAddSns() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();
      testNode.addNode("folder");
      session.save();
      testNode.addNode("folder");
      assertEquals(2, testNode.getNodes().getSize());
      session.save();
      testNode.remove();
      session.save();

      testNode = root.addNode("testNode");
      session.save();

      Node folder = testNode.addNode("folder", "nt:folder");
      session.save();
      assertEquals(1, testNode.getNodes().getSize());
      testNode.addNode("folder");
      session.save();

      assertEquals(2, testNode.getNodes().getSize());
      testNode.addNode("folder", "nt:folder");
      session.save();

      testNode.remove();
      session.save();

   }

   public void testCheckMandatoryField() throws Exception
   {
      root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory property 'exo:mandatoryfield' has not been set");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      Node parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      // Add a child node with a random name
      parent.addNode("foo", "nt:unstructured");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory property 'exo:mandatoryfield' has not been set");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      // Add a child node with the same name as mandatory field
      parent.addNode("exo:mandatoryfield", "nt:unstructured");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory property 'exo:mandatoryfield' has not been set");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      parent.setProperty("exo:mandatoryfield", "foo");
      session.save();

      parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      parent.setProperty("exo:mandatoryfield", "foo");
      // Add a child node with a random name
      parent.addNode("foo", "nt:unstructured");
      session.save();

      parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      parent.setProperty("exo:mandatoryfield", "foo");
      // Add a child node with the same name as mandatory field
      parent.addNode("exo:mandatoryfield", "nt:unstructured");
      session.save();

      //Make sure that it can still fail after some successes
      parent = root.addNode("testCheckMandatoryField", "exo:JCR_2307");
      // Add a child node with the same name as mandatory field
      parent.addNode("exo:mandatoryfield", "nt:unstructured");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory property 'exo:mandatoryfield' has not been set");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);
   }

   public void testCheckMandatoryNode() throws Exception
   {
      root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory node 'exo:mandatorySubNode' has not been added");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      Node parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      // Set a property with a random name
      parent.setProperty("foo", "bar");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory node 'exo:mandatorySubNode' has not been added");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      // Add a property with the same name as mandatory node
      parent.setProperty("exo:mandatorySubNode", "bar");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory node 'exo:mandatorySubNode' has not been added");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);

      parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      parent.addNode("exo:mandatorySubNode");
      session.save();

      parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      parent.addNode("exo:mandatorySubNode");
      // Add a property with a random name
      parent.setProperty("foo", "bar");
      session.save();

      parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      parent.addNode("exo:mandatorySubNode", "nt:unstructured");
      // Add a property with the same name as mandatory node
      parent.setProperty("exo:mandatorySubNode", "foo");
      session.save();

      //Make sure that it can still fail after some successes
      parent = root.addNode("testCheckMandatoryNode", "exo:JCR_2307_2");
      // Add a property with the same name as mandatory node
      parent.setProperty("exo:mandatorySubNode", "bar");
      try
      {
         session.save();
         fail("A ConstraintViolationException is expected as the mandatory node 'exo:mandatorySubNode' has not been added");
      }
      catch (ConstraintViolationException e)
      {
         // expected exception
      }
      session.refresh(false);
   }
}
