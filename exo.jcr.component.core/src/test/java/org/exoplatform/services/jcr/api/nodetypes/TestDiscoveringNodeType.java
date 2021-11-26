/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.api.nodetypes;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestDiscoveringNodeType.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestDiscoveringNodeType extends JcrAPIBaseTest
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
