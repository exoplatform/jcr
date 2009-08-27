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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestAssignMixin.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestAssignMixin
   extends JcrAPIBaseTest
{

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node file = root.addNode("TestAssignMixin", "nt:unstructured");
      session.save();
   }

   public void tearDown() throws Exception
   {
      session = (SessionImpl) repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      root.getNode("TestAssignMixin").remove();
      session.save();

      super.tearDown();
   }

   public void testAddMixin() throws RepositoryException
   {
      Node node = (Node) session.getItem("/TestAssignMixin");

      try
      {
         node.addMixin("nt:notFound");
         fail("exception should have been thrown");
      }
      catch (NoSuchNodeTypeException e)
      {
      }

      try
      {
         node.addMixin("nt:base");
         fail("exception should have been thrown");
      }
      catch (NoSuchNodeTypeException e)
      {
      }

      node.addMixin("mix:referenceable");
      node.save();

      assertNotNull(node.getProperty("jcr:uuid").getString());
      assertEquals("mix:referenceable", node.getMixinNodeTypes()[0].getName());
   }

   public void testCanAddMixin() throws RepositoryException
   {

      Node node = (Node) session.getItem("/TestAssignMixin");

      log.debug(">>>>>>>" + node.hasProperty("jcr:mixinTypes"));
      assertTrue(node.canAddMixin("mix:referenceable"));
      node.addMixin("mix:referenceable");
      assertFalse(node.canAddMixin("mix:referenceable"));
   }

   public void testRemoveMixin() throws RepositoryException
   {
      Node node = (Node) session.getItem("/TestAssignMixin");
      node.addMixin("mix:referenceable");
      assertEquals(1, node.getMixinNodeTypes().length);
      // node.save();
      node.removeMixin("mix:referenceable");
      assertEquals(0, node.getMixinNodeTypes().length);

      // node.save();
   }

   public void testAddCustomMixinAfterNodeSave() throws Exception
   {
      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeType(createTestMixinValue(), 0);

      Node node = root.addNode("testAddCustomMixinAfterNodeSave", "nt:base");
      root.save();
      node.addMixin("exo:myMixin");
      assertEquals("myTestProp", node.getMixinNodeTypes()[0].getPropertyDefinitions()[0].getName());

      node.setProperty("myTestProp", "test");
      node.save();
      node = session.getRootNode().getNode("testAddCustomMixinAfterNodeSave");
      assertEquals("nt:base", node.getPrimaryNodeType().getName());
      assertEquals("exo:myMixin", node.getMixinNodeTypes()[0].getName());
      assertEquals("myTestProp", node.getProperty("myTestProp").getDefinition().getName());

   }

   // prerequisites: CMS nodes should be registered
   public void testAddCustomMixinBeforeNodeSave() throws Exception
   {

      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeType(createTestMixinValue1(), 0);

      Node node = root.addNode("testAddCustomMixinBeforeNodeSave", "nt:folder");
      root.save();
      node.addMixin("exo:myMixin1");

      node.addNode("child1", "nt:unstructured");
      node.save();

      // node = session.getRootNode().getNode("testNode");
      // assertEquals("nt:base", node.getPrimaryNodeType().getName());
      // assertEquals("exo:myMixin", node.getMixinNodeTypes()[0].getName());
      // assertEquals("myTestProp", node.getProperty("myTestProp").getDefinition().getName());

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

   private NodeTypeValue createTestMixinValue1()
   {
      NodeTypeValue testNtValue = new NodeTypeValue();
      testNtValue.setName("exo:myMixin1");
      testNtValue.setMixin(true);
      testNtValue.setOrderableChild(false);
      testNtValue.setPrimaryItemName(null);
      ArrayList supertypes = new ArrayList();
      // supertypes.add("nt:base");
      // supertypes.add("mix:referenceable");
      testNtValue.setDeclaredSupertypeNames(supertypes);

      ArrayList props = new ArrayList();
      PropertyDefinitionValue prop1 = new PropertyDefinitionValue();
      prop1.setAutoCreate(false);
      ArrayList defVals = new ArrayList();
      prop1.setDefaultValueStrings(defVals);
      prop1.setMandatory(false);
      prop1.setMultiple(false);
      prop1.setName("*");
      prop1.setOnVersion(OnParentVersionAction.COPY);
      prop1.setReadOnly(false);
      prop1.setRequiredType(PropertyType.UNDEFINED);
      props.add(prop1);
      ArrayList constraints = new ArrayList();
      prop1.setValueConstraints(constraints);

      PropertyDefinitionValue prop2 = new PropertyDefinitionValue();
      prop2.setName("exo:multiProperty");
      prop2.setRequiredType(PropertyType.UNDEFINED);
      prop2.setAutoCreate(false);
      prop2.setMandatory(false);
      prop2.setMultiple(true);
      prop2.setOnVersion(OnParentVersionAction.COPY);
      prop2.setReadOnly(false);
      prop2.setValueConstraints(constraints);
      prop2.setDefaultValueStrings(defVals);
      props.add(prop2);

      testNtValue.setDeclaredPropertyDefinitionValues(props);

      ArrayList nodes = new ArrayList();
      NodeDefinitionValue node1 = new NodeDefinitionValue();
      node1.setName("*");
      node1.setAutoCreate(false);
      // node1.setDefaultNodeTypeName("test:setProperty");
      node1.setDefaultNodeTypeName("nt:unstructured");
      node1.setMandatory(false);
      node1.setSameNameSiblings(false);
      node1.setOnVersion(OnParentVersionAction.COPY);
      node1.setReadOnly(false);
      ArrayList nodeTypes = new ArrayList();
      nodeTypes.add("nt:unstructured");
      node1.setRequiredNodeTypeNames(nodeTypes);
      nodes.add(node1);
      testNtValue.setDeclaredChildNodeDefinitionValues(nodes);

      return testNtValue;
   }

}
