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

import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.OnParentVersionAction;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestCopyNode.java 13891 2008-05-05 16:02:30Z pnedonosko $
 */
public class TestCopyNode
   extends JcrAPIBaseTest
{

   public void setUp() throws Exception
   {
      super.setUp();

      // TODO
      // if(!((RepositoryImpl) repository).isWorkspaceInitialized("ws2"));
      // ((RepositoryImpl) repository).initWorkspace("ws2", "nt:unstructured");
   }

   public void testSessionMove() throws RepositoryException
   {
      Node root;
      try
      {
         session.move("/testSessionMove1", "/dummyNode");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

      root = session.getRootNode();
      Node file = root.addNode("testSessionMove", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      session.move("/testSessionMove", "/testSessionMove1");
      // log.debug(" root's nodes >>>> "+session.getRootNode());
      log.debug("DataManager: \n" + session.getTransientNodesManager().dump());

      session.save();

      log.debug(" root's nodes >>>> " + session.getRootNode().getNodes());
      // log.debug("DataManager: \n"+session.getTransientNodesManager().dump());
      // log.debug("System DataManager: \n"+((RepositoryImpl)session.getRepository()).
      // getSystemSession().getTransientNodesManager().dump());

      session = (SessionImpl) repository.login(credentials, WORKSPACE);

      log.debug(" root's nodes >>>> " + session.getRootNode().getNodes().getSize());

      assertNotNull(session.getItem("/testSessionMove1"));
      assertNotNull(session.getItem("/testSessionMove1/childNode2/jcr:content"));

      try
      {
         session.getItem("/testSessionMove");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();

      try
      {
         session.move("/toCorrupt", "/testSessionMove/corrupted");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("testSessionMove1").remove();
      session.getRootNode().getNode("existNode").remove();
      // session.getRootNode().getNode("childNode").remove();
      session.save();

   }

   public void testCopy() throws Exception
   {

      try
      {
         workspace.copy("/dummyNode", "/testCopy1");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

      Node root = session.getRootNode();
      Node file = root.addNode("testCopy", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      workspace.copy("/testCopy", "/testCopy1");

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      assertNotNull(session.getItem("/testCopy1"));
      assertNotNull(session.getItem("/testCopy1/childNode2"));
      assertNotNull(session.getItem("/testCopy1/childNode2/jcr:content"));
      assertNotNull(session.getItem("/testCopy"));

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();
      try
      {
         workspace.copy("/toCorrupt", "/test/childNode/corrupted");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      session.getRootNode().getNode("testCopy1").remove();
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("existNode").remove();
      session.getRootNode().getNode("testCopy").remove();
      session.save();
   }

   public void testMove() throws Exception
   {
      try
      {
         workspace.move("/dummyNode", "/testMove1");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

      Node root = session.getRootNode();
      Node file = root.addNode("testMove", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      workspace.move("/testMove", "/testMove1");

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      assertNotNull(session.getItem("/testMove1"));
      assertNotNull(session.getItem("/testMove1/childNode2"));
      assertNotNull(session.getItem("/testMove1/childNode2/jcr:content"));

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();

      try
      {
         session.getItem("/testMove");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      try
      {
         workspace.move("/toCorrupt", "/test/childNode/corrupted");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      session.getRootNode().getNode("testMove1").remove();
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("existNode").remove();
      session.save();
   }

   public void testMoveTransient() throws Exception
   {
      Node testRoot = root.addNode("test_move_transient");
      Node source = testRoot.addNode("Source node");
      session.save();

      Node child1 = source.addNode("Child 1");
      Node child2 = source.addNode("Child 2");
      source.save();

      Node child3_transient = source.addNode("Child 3");

      // test case
      try
      {
         session.move(source.getPath(), testRoot.getPath() + "/Destenation node");
         session.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("In-session move of a parent with pending changes (transient items) fails " + e);
      }

      // check if exists
      try
      {
         Node transientItemMustExists = root.getNode(testRoot.getName() + "/Destenation node/Child 3");
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Transient item is not moved to a new location, transient: " + child3_transient.getPath() + ". " + e);
      }

      // tear down
      testRoot.remove();
      session.save();
   }

   public void testMoveReferenceable() throws Exception
   {
      Node testRoot = root.addNode("test_move_transient");
      Node source = testRoot.addNode("Source node");
      source.addMixin("mix:referenceable");
      session.save();

      Node child1 = source.addNode("Child 1");
      Node child2 = source.addNode("Child 2");
      source.save();

      Node refHolder = testRoot.addNode("Holder node");
      Property refProp = refHolder.setProperty("Ref property", source);
      session.save();

      // test case
      try
      {
         session.move(source.getPath(), testRoot.getPath() + "/Destenation node");
         session.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("In-session move of a referenceable node fails " + e);
      }

      // check if exists
      try
      {
         assertEquals("Referenceable node has different UUID after the move operation", source.getUUID(), testRoot
                  .getNode("Destenation node").getUUID());
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("A node must be is referenceable after the move operation, node: " + source.getPath() + ". " + e);
      }

      // tear down
      testRoot.remove();
      session.save();
   }

   public void testCopyFromDifferentWS() throws Exception
   {

      try
      {
         workspace.copy("ws2", "/dummyNode", "/testCopyFromDifferentWS1");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }
      Session session2 = repository.login(credentials, "ws2");
      Node root = session2.getRootNode();
      Node file = root.addNode("testCopyFromDifferentWS", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session2.save();

      root = session.getRootNode();
      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      workspace.copy("ws2", "/testCopyFromDifferentWS", "/testCopyFromDifferentWS1");

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      assertNotNull(session.getItem("/testCopyFromDifferentWS1"));
      assertNotNull(session.getItem("/testCopyFromDifferentWS1/childNode2"));
      assertNotNull(session.getItem("/testCopyFromDifferentWS1/childNode2/jcr:content"));

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();
      try
      {
         workspace.copy("ws2", "/toCorrupt", "/test/childNode/corrupted");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      session.getRootNode().getNode("testCopyFromDifferentWS1").remove();
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("existNode").remove();
      // session.getRootNode().getNode("childNode").remove();
      session.save();
      session2.getRootNode().getNode("testCopyFromDifferentWS").remove();
      session2.save();
   }

   public void testClone() throws Exception
   {

      try
      {
         workspace.clone("ws2", "/dummyNode", "/testClone1", false);
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }
      Session session2 = repository.login(credentials, "ws2");
      Node root = session2.getRootNode();
      Node file = root.addNode("testClone", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
               PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session2.save();
      // cache pb
      // session2.getItem("/childNode");

      root = session.getRootNode();
      root.addNode("existNode", "nt:unstructured").addNode("childNode", "nt:unstructured");
      // root.addNode("test", "nt:unstructured");
      session.save();

      log.debug("CLONE >>");
      workspace.clone("ws2", "/testClone", "/testClone1", false);

      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      assertNotNull(session.getItem("/testClone1"));
      assertNotNull(session.getItem("/testClone1/childNode2"));
      assertNotNull(session.getItem("/testClone1/childNode2/jcr:content"));

      session2 = repository.login(credentials, "ws2");
      assertEquals(((Node) session.getItem("/testClone1/childNode2/jcr:content")).getUUID(), ((Node) session2
               .getItem("/testClone/childNode2/jcr:content")).getUUID());

      session.getRootNode().addNode("toCorrupt", "nt:unstructured");
      session.save();
      try
      {
         workspace.clone("ws2", "/toCorrupt", "/testClone1/childNode/corrupted", false);
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      session.getRootNode().getNode("testClone1").remove();
      session.getRootNode().getNode("toCorrupt").remove();
      session.getRootNode().getNode("existNode").remove();
      session.save();
      session2.getRootNode().getNode("testClone").remove();
      session2.save();
   }

   public void testCloneWithMixin() throws RepositoryException
   {
      Session session2 = repository.login(credentials, "ws2");
      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl) session2.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeType(createTestMixinValue(), 0);

      Node root = session2.getRootNode();
      Node node = root.addNode("clonedNode", "nt:base");
      node.addMixin("mix:referenceable");
      session2.save();
      node = root.getNode("clonedNode");
      node.addMixin("exo:myMixin");
      node.setProperty("myTestProp", "myProp");
      session2.save();

      // root = session.getRootNode();
      // root.addNode("test", "nt:unstructured");
      // session.save();

      workspace.clone("ws2", "/clonedNode", "/test1", false);

      // log.debug("CLONE W/ MIXIN>> "+);

      assertEquals(((Node) session2.getItem("/clonedNode")).getUUID(), ((Node) session.getItem("/test1")).getUUID());

   }

   public void testCloneWithMixinAndRemoveExisting() throws RepositoryException
   {
      Session session2 = repository.login(credentials, "ws2");
      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl) session2.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeType(createTestMixinValue(), 0);

      Node root = session2.getRootNode();
      Node node = root.addNode("clonedNode1", "nt:base");
      node.addMixin("mix:referenceable");
      session2.save();
      node = root.getNode("clonedNode1");
      node.addMixin("exo:myMixin");
      node.setProperty("myTestProp", "myProp");
      session2.save();

      root = session.getRootNode();
      workspace.clone("ws2", "/clonedNode1", "/xx3", true);

      assertNotNull(((Node) session.getItem("/xx3")).getUUID());
   }

   private NodeTypeValue createTestMixinValue()
   {
      NodeTypeValue testNtValue = new NodeTypeValue();
      testNtValue.setName("exo:myMixin");
      testNtValue.setMixin(true);
      testNtValue.setOrderableChild(false);
      testNtValue.setPrimaryItemName(null);
      ArrayList supertypes = new ArrayList();
      // supertypes.add("nt:base");
      testNtValue.setDeclaredSupertypeNames(supertypes);

      ArrayList props = new ArrayList();
      PropertyDefinitionValue prop1 = new PropertyDefinitionValue();
      prop1.setAutoCreate(false);
      ArrayList defVals = new ArrayList();
      defVals.add("test");
      prop1.setDefaultValueStrings(defVals);
      prop1.setMandatory(false);
      prop1.setMultiple(false);
      prop1.setName("myTestProp");
      prop1.setOnVersion(OnParentVersionAction.IGNORE);
      prop1.setReadOnly(false);
      prop1.setRequiredType(PropertyType.STRING);
      ArrayList constraints = new ArrayList();
      prop1.setValueConstraints(constraints);
      props.add(prop1);
      testNtValue.setDeclaredPropertyDefinitionValues(props);

      ArrayList nodes = new ArrayList();
      testNtValue.setDeclaredChildNodeDefinitionValues(nodes);

      return testNtValue;
   }
}
