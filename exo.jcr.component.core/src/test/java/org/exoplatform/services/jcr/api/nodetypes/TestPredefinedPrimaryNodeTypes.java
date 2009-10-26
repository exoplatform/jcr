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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.EntityCollection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestPredefinedPrimaryNodeTypes.java 11907 2008-03-13 15:36:21Z
 *          ksm $
 */

public class TestPredefinedPrimaryNodeTypes extends JcrAPIBaseTest
{

   public void testUnstructured() throws Exception
   {
      Node node = root.addNode("node1", "nt:unstructured");
      NodeDefinition def = node.getDefinition();
      NodeType type = node.getPrimaryNodeType();

      assertTrue("have child ", ((EntityCollection)node.getNodes()).size() == 0);
      assertTrue("prop num !=1 ", ((EntityCollection)node.getProperties()).size() == 1);
      assertEquals("Prop not default ", "nt:unstructured", node.getProperty("jcr:primaryType").getString());

      assertEquals("Type!= nt:unstructured", type.getName(), "nt:unstructured");
      NodeDefinition[] childNodeDefs = type.getChildNodeDefinitions();
      PropertyDefinition[] propertyDefinitions = type.getPropertyDefinitions();

      assertTrue("typeNodeDefs != 1", childNodeDefs.length == 1);
      assertTrue("typePropDefs != 4", propertyDefinitions.length == 4);

      // assertEquals("prop!=jcr:primaryType", "jcr:primaryType",
      // type.getPropertyDefinitions()[1].getName());
      assertTrue(containsDefinition(Constants.JCR_ANY_NAME.getName(), propertyDefinitions));
      assertTrue(containsDefinition(Constants.JCR_ANY_NAME.getName(), childNodeDefs));

   }

   private boolean containsDefinition(String name, ItemDefinition[] defs)
   {
      for (int i = 0; i < defs.length; i++)
      {
         if (name.equals(defs[i].getName()))
            return true;
      }
      return false;
   }

   public void testHierarchyNode() throws Exception
   {

      Node node = root.addNode("node-hi", "nt:hierarchyNode");
      NodeDefinition def = node.getDefinition();
      NodeType type = node.getPrimaryNodeType();

      assertTrue("have child ", ((EntityCollection)node.getNodes()).size() == 0);
      assertTrue("prop num !=2 ==" + ((EntityCollection)node.getProperties()).size(), ((EntityCollection)node
         .getProperties()).size() == 2);

      PropertyDefinition[] propertyDefinitions = type.getPropertyDefinitions();
      assertTrue("typePropDefs != 3", propertyDefinitions.length == 3);
      // NodeDefs = null
      assertTrue("nodeDefs != 0", type.getChildNodeDefinitions().length == 0);

      // Property names: [0]=jcr:created, [1]=jcr:lastModified,
      // [2]=jcr:primaryType
      assertTrue("prop2 name !=jcr:primaryType", containsDefinition("jcr:primaryType", propertyDefinitions));
      assertTrue("prop0 name != jcr:created", containsDefinition("jcr:created", propertyDefinitions));

      node = root.getNode("node-hi");
      assertNotNull("Prop null ", node.getProperty("jcr:created").toString());
      // assertNull("Prop modified SAVED not null ",
      // node.getProperty("jcr:lastModified").getValue());
   }

   public void testFile() throws Exception
   {

      Node node = root.addNode("node-f", "nt:file");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("Type!= nt:file", "nt:file", type.getName());
      assertTrue("typePropDefs != 3", type.getPropertyDefinitions().length == 3);
      assertTrue("typeNodeDefs != 1", type.getChildNodeDefinitions().length == 1);

      // Property names: [0]=jcr:created, [2]=jcr:primaryType
      assertEquals("node0 name != jcr:content", "jcr:content", type.getChildNodeDefinitions()[0].getName());

      try
      {
         node.addNode("not-allowed");
         fail("AddNode ConstraintViolationException should be thrown!");
      }
      catch (ConstraintViolationException e)
      {
      }

      try
      {
         node.setProperty("not-allowed", "val");
         node.save();
         fail("SetProp ConstraintViolationException should be thrown!");
      }
      catch (RepositoryException e)
      {
      }

   }

   public void testFolder() throws Exception
   {

      Node node = root.addNode("node-fl", "nt:folder");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("Type!= nt:folder", "nt:folder", type.getName());
      assertTrue("typePropDefs != 3", type.getPropertyDefinitions().length == 3);
      assertTrue("typeNodeDefs != 1", type.getChildNodeDefinitions().length == 1);

      NodeDefinition[] childNodeDefs = type.getChildNodeDefinitions();
      assertTrue(containsDefinition(Constants.JCR_ANY_NAME.getName(), childNodeDefs));

      try
      {
         node.setProperty("not-allowed", "val");
         node.save();
         fail("SetProp ConstraintViolationException should be thrown!");
      }
      catch (RepositoryException e)
      {
      }

   }

   public void testMimeResource() throws Exception
   {

      Node node = root.addNode("node-mr", "nt:resource");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("Type!=nt:resource", "nt:resource", type.getName());
      PropertyDefinition[] propDefs = type.getPropertyDefinitions();

      // 4 + primaryType, mixinType, uuid
      assertTrue("typePropDefs = " + type.getPropertyDefinitions().length, type.getPropertyDefinitions().length == 7);
      assertTrue("typeNodeDefs != 0", type.getChildNodeDefinitions().length == 0);

   }

   public void testLinkedFile() throws Exception
   {

      Node node = root.addNode("node-lf", "nt:linkedFile");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("nt:linkedFile", type.getName());
      PropertyDefinition[] propertyDefinitions = type.getPropertyDefinitions();
      assertTrue("typePropDefs != 4", propertyDefinitions.length == 4);
      assertTrue("typeNodeDefs != 0", type.getChildNodeDefinitions().length == 0);

      assertTrue("node0 name != jcr:content", containsDefinition("jcr:content", propertyDefinitions));

   }

   public void testNodeType() throws Exception
   {

      Node node = root.addNode("node-nt", "nt:nodeType");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("nt:nodeType", type.getName());

      assertTrue(type.getPropertyDefinitions().length == 7);
      assertTrue(type.getChildNodeDefinitions().length == 2);

   }

   public void testPropertyDef() throws Exception
   {

      Node node = root.addNode("node-pd", "nt:propertyDefinition");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("nt:propertyDefinition", type.getName());

      assertTrue(type.getPropertyDefinitions().length == 11);
      assertTrue(type.getChildNodeDefinitions().length == 0);

   }

   public void testChildNodeDef() throws Exception
   {

      Node node = root.addNode("node-cnd", "nt:childNodeDefinition");
      NodeType type = node.getPrimaryNodeType();

      assertEquals("nt:childNodeDefinition", type.getName());

      assertTrue(type.getPropertyDefinitions().length == 10);
      assertTrue(type.getChildNodeDefinitions().length == 0);

   }
}
