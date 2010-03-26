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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestSuperTypeChanges extends AbstractNodeTypeTest
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TestSuperTypeChanges");

   /**
    * @throws Exception
    */
   public void testAddVersionableSuper() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testAddVersionableSuper");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();
      assertFalse(testNode.isNodeType("mix:versionable"));
      superType = testNValue.getDeclaredSupertypeNames();
      superType.add("mix:versionable");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      assertTrue(testNode.isNodeType("mix:versionable"));
      testNode.checkin();
      testNode.checkout();
      testNode.remove();

      testNode = root.addNode("testNode", testNValue.getName());
      session.save();
      assertTrue(testNode.isNodeType("mix:versionable"));
   }

   /**
    * @throws Exception
    */
   public void testRemoveVersionableSuper() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      superType.add("mix:versionable");
      testNValue.setName("exo:testRemoveVersionableSuper");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();
      assertTrue(testNode.isNodeType("mix:versionable"));

      superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setDeclaredSupertypeNames(superType);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }

      testNode.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      testNode = root.addNode("testNode", testNValue.getName());
      session.save();
      assertFalse(testNode.isNodeType("mix:versionable"));
   }

   /**
    * @throws Exception
    */
   public void testSplitNT() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:SplitNT");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("jcr:mimeType1", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      props.add(new PropertyDefinitionValue("jcr:mimeType2", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));

      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", testNValue.getName());
      tNode.setProperty("jcr:mimeType1", "plain/text");
      tNode.setProperty("jcr:mimeType2", "plain/html");
      session.save();
      Property property = tNode.getProperty("jcr:mimeType1");
      assertEquals("plain/text", property.getString());
      session.save();

      NodeTypeValue testNValue2 = new NodeTypeValue();
      List<String> superType2 = new ArrayList<String>();
      superType2.add("nt:base");
      testNValue2.setName("exo:SplitNT2");
      testNValue2.setPrimaryItemName("");
      testNValue2.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props2 = new ArrayList<PropertyDefinitionValue>();

      props2.add(new PropertyDefinitionValue("jcr:mimeType2", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));

      testNValue2.setDeclaredPropertyDefinitionValues(props);

      nodeTypeManager.registerNodeType(testNValue2, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      superType = new ArrayList<String>();
      superType.add("nt:base");
      superType.add(testNValue2.getName());
      testNValue.setDeclaredSupertypeNames(superType);

      props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("jcr:mimeType1", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

}
