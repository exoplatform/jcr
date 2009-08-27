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

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestDiscoveringNodeType.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestDiscoveringNodeType
   extends JcrAPIBaseTest
{

   public void testGetPrimaryNodeType() throws Exception
   {

      Node node = root.addNode("node1", "nt:unstructured");

      NodeType type = node.getPrimaryNodeType();
      assertEquals("nt:unstructured", type.getName());

   }

   public void testGetMixinNodeTypes() throws Exception
   {

      Node node = root.addNode("node1", "nt:unstructured");
      assertEquals(0, node.getMixinNodeTypes().length);

      node.addMixin("mix:referenceable");
      assertEquals(1, node.getMixinNodeTypes().length);

      NodeType type = node.getMixinNodeTypes()[0];
      assertEquals("mix:referenceable", type.getName());

   }

   public void testIsNodeType() throws Exception
   {

      Node node = root.addNode("node1", "nt:unstructured");
      assertFalse(node.isNodeType("mix:referenceable"));
      node.addMixin("mix:referenceable");

      assertTrue(node.isNodeType("nt:unstructured"));
      assertTrue("Not nt:base", node.isNodeType("nt:base"));
      assertTrue(node.isNodeType("mix:referenceable"));

      assertFalse(node.isNodeType("nt:file"));
      assertFalse(node.isNodeType("mix:notfound"));

   }

}
