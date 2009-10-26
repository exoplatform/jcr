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
package org.exoplatform.services.jcr.api.nodetypes;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestDiscoveringNodeTypeDefinition.java 11907 2008-03-13
 *          15:36:21Z ksm $
 */

public class TestDiscoveringNodeTypeDefinition extends JcrAPIBaseTest
{

   /*
    * removed as hard to support test
    */
   public void _testPrimaryNodeTypeDefinitionProperties() throws Exception
   {
      Node node = root.addNode("node1", "nt:resource");
      NodeType type = node.getPrimaryNodeType();
      assertEquals("nt:resource", type.getName());
      assertEquals(false, type.isMixin());
      assertEquals(false, type.hasOrderableChildNodes());
      assertEquals("jcr:data", type.getPrimaryItemName());
      assertEquals(2, type.getSupertypes().length);
      assertEquals(2, type.getDeclaredSupertypes().length);
      assertEquals(true, type.isNodeType("nt:base"));
      assertEquals(false, type.isNodeType("nt:file"));
      assertEquals(7, type.getPropertyDefinitions().length);
      assertEquals(4, type.getDeclaredPropertyDefinitions().length);
      assertEquals(0, type.getChildNodeDefinitions().length);
      assertEquals(0, type.getDeclaredChildNodeDefinitions().length);
   }

   public void testMixinNodeTypeDefinitionProperties() throws Exception
   {
      Node node = root.addNode("node1");
      node.addMixin("mix:referenceable");
      assertEquals(1, node.getMixinNodeTypes().length);
      NodeType type = node.getMixinNodeTypes()[0];
      assertEquals("mix:referenceable", type.getName());
      assertEquals(true, type.isMixin());
      assertEquals(false, type.hasOrderableChildNodes());
      assertNull(type.getPrimaryItemName());
      assertEquals(0, type.getSupertypes().length);
      assertEquals(0, type.getDeclaredSupertypes().length);
      assertEquals(false, type.isNodeType("nt:base"));
      assertEquals(1, type.getPropertyDefinitions().length);
      assertEquals(1, type.getDeclaredPropertyDefinitions().length);
      assertEquals(0, type.getChildNodeDefinitions().length);
      assertEquals(0, type.getDeclaredChildNodeDefinitions().length);
   }

   public void testCanModify() throws Exception
   {
      Node node = root.addNode("node1", "nt:resource");
      NodeType type = node.getPrimaryNodeType();
      assertFalse(type.canAddChildNode("jcr:anyNode"));
      assertFalse(type.canAddChildNode("jcr:anyNode", "nt:base"));
      // assertTrue(type.canSetProperty("jcr:data", new BinaryValue("test")));
      assertFalse(type.canSetProperty("jcr:data", new BinaryValue[]{new BinaryValue("test")}));
      assertFalse(type.canSetProperty("jcr:notFound", new BinaryValue("test")));
      // [PN] 06.03.06 Row below commented
      // assertFalse(type.canSetProperty("jcr:data", new StringValue("test")));
      assertFalse(type.canRemoveItem("jcr:data"));
      assertFalse(type.canRemoveItem("jcr:notFound"));

      node = root.addNode("node2", "nt:file");
      type = node.getPrimaryNodeType();
      // does not work - TODO
      // assertTrue(type.canAddChildNode("jcr:content"));
      assertTrue(type.canAddChildNode("jcr:content", "nt:unstructured"));
      assertFalse(type.canAddChildNode("jcr:othernode"));
      assertTrue(type.canAddChildNode("jcr:content", "nt:unstructured"));
      assertFalse(type.canAddChildNode("jcr:content", "mix:referenceable"));

      // root.getNode("node2").remove();
      node = root.addNode("node3", "nt:folder");
      type = node.getPrimaryNodeType();
      // Residual,
      // 6.7.22.8 nt:folder, ChildNodeDefinition, Name *
      // RequiredPrimaryType[nt:hierarchyNode]
      assertTrue(type.canAddChildNode("jcr:content", "nt:hierarchyNode"));
      assertFalse(type.canAddChildNode("jcr:othernode"));

      // does not work - TODO
      // assertTrue(type.canAddChildNode("jcr:content", "nt:unstructured"));
      // assertTrue(type.canAddChildNode("jcr:content", "mix:referenceable"));

   }
}
