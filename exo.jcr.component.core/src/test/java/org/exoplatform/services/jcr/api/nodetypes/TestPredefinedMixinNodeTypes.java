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
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestPredefinedMixinNodeTypes.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestPredefinedMixinNodeTypes extends JcrAPIBaseTest
{

   public void testReferenceable() throws Exception
   {

      Node node;
      try
      {
         node = root.addNode("node-h", "mix:referenceable");
         fail("AddNode ConstraintViolationException should be thrown as type is not primary!");
      }
      catch (ConstraintViolationException e)
      {
      }

      node = root.addNode("node-h", "nt:unstructured");

      node.addMixin("mix:referenceable");
      assertEquals(1, node.getMixinNodeTypes().length);
      assertEquals("mix:referenceable", node.getMixinNodeTypes()[0].getName());
      assertEquals("nt:unstructured", node.getPrimaryNodeType().getName());

      assertNotNull(node.getProperty("jcr:uuid").toString());
      assertEquals("jcr:uuid", node.getProperty("jcr:uuid").getDefinition().getName());
      assertTrue(node.getProperty("jcr:mixinTypes").getDefinition().isProtected());
      assertTrue(node.getProperty("jcr:mixinTypes").getDefinition().isMultiple());

      assertTrue(node.getProperty("jcr:uuid").getDefinition().isProtected());
      assertFalse(node.getProperty("jcr:uuid").getDefinition().isMultiple());

      root.save();
      node = root.getNode("node-h");
      assertNotNull("Prop not null ", node.getProperty("jcr:uuid").toString());

      // UUID Read Only
      try
      {
         node.setProperty("jcr:uuid", "1234");
         node.save();
         fail("SetProp UUID ConstraintViolationException should be thrown!");
      }
      catch (ConstraintViolationException e)
      {
      }
   }

}
