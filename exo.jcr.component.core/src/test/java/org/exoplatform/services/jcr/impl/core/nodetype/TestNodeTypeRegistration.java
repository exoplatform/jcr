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
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestNodeTypeRegistration extends AbstractNodeTypeTest
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TestNodeTypeRegistration");

   private NodeTypeValue testNodeTypeValue = null;

   private NodeTypeValue testNodeTypeValue2 = null;

   private NodeTypeValue testNtFileNodeTypeValue = null;

   /**
    * 
    */
   public TestNodeTypeRegistration()
   {
      super();
      testNodeTypeValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNodeTypeValue.setName("exo:testRegistrationNodeType");
      testNodeTypeValue.setPrimaryItemName("");
      testNodeTypeValue.setDeclaredSupertypeNames(superType);

      testNodeTypeValue2 = new NodeTypeValue();
      List<String> superType2 = new ArrayList<String>();
      superType2.add("nt:base");
      superType2.add(testNodeTypeValue.getName());
      testNodeTypeValue2.setName("exo:testRegistrationNodeType2");
      testNodeTypeValue2.setPrimaryItemName("");
      testNodeTypeValue2.setDeclaredSupertypeNames(superType2);

      testNtFileNodeTypeValue = new NodeTypeValue();
      List<String> superType3 = new ArrayList<String>();
      superType3.add("nt:base");
      testNtFileNodeTypeValue.setName("nt:file");
      testNtFileNodeTypeValue.setPrimaryItemName("");
      testNtFileNodeTypeValue.setDeclaredSupertypeNames(superType3);

   }

   public void testRemoveNodeTypeUnexisted()
   {
      try
      {
         nodeTypeManager.unregisterNodeType("blah-blah");
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
   }

   /**
    * Test remove of build in node type
    */
   public void testRemoveBuildInNodeType()
   {
      try
      {
         nodeTypeManager.unregisterNodeType("nt:base");
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
   }

   /**
    * @throws RepositoryException
    */
   public void testRemoveSuperNodeType() throws RepositoryException
   {
      nodeTypeManager.registerNodeType(testNodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      nodeTypeManager.registerNodeType(testNodeTypeValue2, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      try
      {
         nodeTypeManager.unregisterNodeType(testNodeTypeValue.getName());
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
      nodeTypeManager.unregisterNodeType(testNodeTypeValue2.getName());
      nodeTypeManager.unregisterNodeType(testNodeTypeValue.getName());
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();

   }

   /**
    * @throws Exception
    */
   public void testRemoveNodeTypeExistedNode() throws Exception
   {
      nodeTypeManager.registerNodeType(testNodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      Node testNode = root.addNode("test", testNodeTypeValue.getName());
      assertTrue(testNode.isNodeType(testNodeTypeValue.getName()));
      session.save();
      try
      {
         nodeTypeManager.unregisterNodeType(testNodeTypeValue.getName());
         fail("");
      }
      catch (RepositoryException e)
      {
         // ok
      }
      testNode.remove();
      session.save();
      nodeTypeManager.unregisterNodeType(testNodeTypeValue.getName());
   }

   public void testReregisterBuildInNodeType() throws Exception
   {
      try
      {
         nodeTypeManager.registerNodeType(testNtFileNodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
   }

   /**
    * Remove residual property definition. Cover
    * PropertyDefinitionComparator.validateRemoved method.
    * 
    * @throws Exception
    */
   public void testReregisterResidual() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testRemoveResidual");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false, 0,
         new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      assertTrue(nodeTypeManager.getNodeType(testNValue.getName()).getDeclaredPropertyDefinitions().length == 1);

      Node tNode = root.addNode("test", "exo:testRemoveResidual");
      Property prop = tNode.setProperty("tt", "tt");
      session.save();

      testNValue.setDeclaredPropertyDefinitionValues(new ArrayList<PropertyDefinitionValue>());

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }

      prop.remove();
      session.save();
      assertTrue(nodeTypeManager.getNodeType(testNValue.getName()).getDeclaredPropertyDefinitions().length == 1);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      assertTrue(nodeTypeManager.getNodeType(testNValue.getName()).getDeclaredPropertyDefinitions().length == 0);
      tNode.remove();
      session.save();
      nodeTypeManager.unregisterNodeType(testNValue.getName());
   }

   /**
    * Cover part of the PropertyDefinitionComparator.doChanged method.
    * 
    * @throws Exception
    */
   public void _testReregisterProtected() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testChangeProtected");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      List<String> def = new ArrayList<String>();
      def.add("tt");
      props.add(new PropertyDefinitionValue("tt", true, false, 1, false, def, false, PropertyType.STRING,
         new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", "exo:testChangeProtected");
      session.save();
      Property property = tNode.getProperty("tt");
      assertEquals("tt", property.getString());

      property.remove();
      session.save();

      tNode.addMixin("mix:versionable");

      // chenge protected
      List<PropertyDefinitionValue> props2 = new ArrayList<PropertyDefinitionValue>();
      props2.add(new PropertyDefinitionValue("tt", true, false, 1, true, def, false, PropertyType.STRING,
         new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props2);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      tNode.setProperty("tt", "tt");
      session.save();

      property = tNode.getProperty("tt");
      assertEquals("tt", property.getString());
      try
      {
         property.remove();
         session.save();
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }
   }

   /**
    * Cover PropertyDefinitionComparator.validateAdded method.
    * 
    * @throws Exception
    */
   public void testReregisterAddNewProperty() throws Exception
   {
      NodeTypeValue testNTValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNTValue.setName("exo:testReregisterAddNewProperty");
      testNTValue.setPrimaryItemName("");
      testNTValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNTValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      Node testNode = root.addNode("testNode", testNTValue.getName());
      session.save();

      testNTValue = nodeTypeManager.getNodeTypeValue(testNTValue.getName());
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("tt", true, true, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNTValue.setDeclaredPropertyDefinitionValues(props);

      try
      {
         nodeTypeManager.registerNodeType(testNTValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }
      testNTValue = nodeTypeManager.getNodeTypeValue(testNTValue.getName());
      List<String> def = new ArrayList<String>();
      def.add("tt");
      props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("tt", true, true, 1, false, def, false, PropertyType.STRING,
         new ArrayList<String>()));
      testNTValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNTValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      assertEquals("tt", testNode.getProperty("tt").getString());
      Node test2 = root.addNode("test2", testNTValue.getName());
      assertEquals("tt", test2.getProperty("tt").getString());
   }

   /**
    * Cover part of the PropertyDefinitionComparator.doChanged method.
    * 
    * @throws Exception
    */
   public void testReregisterMandatory() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterMandatory");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("tt", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node tNode = root.addNode("test", "exo:testReregisterMandatory");
      session.save();

      // chenge mandatory
      List<PropertyDefinitionValue> props2 = new ArrayList<PropertyDefinitionValue>();
      props2.add(new PropertyDefinitionValue("tt", false, true, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props2);
      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }

      tNode.setProperty("tt", "tt");
      session.save();

      Property property = tNode.getProperty("tt");
      assertEquals("tt", property.getString());

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterRequiredNodeTypeChangeResidualProperty() throws Exception
   {
      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRequiredNodeTypeChangeResidualProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.UNDEFINED, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", "exo:testReregisterRequiredNodeTypeChangeResidualProperty");
      tNode.setProperty("tt", "tt");
      tNode.setProperty("t2", 1);
      tNode.setProperty("t3", Calendar.getInstance());
      session.save();

      // chenge mandatory
      List<PropertyDefinitionValue> props2 = new ArrayList<PropertyDefinitionValue>();
      props2.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props2);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }
      tNode.remove();
      session.save();

      tNode = root.addNode("test", "exo:testReregisterRequiredNodeTypeChangeResidualProperty");
      tNode.setProperty("tt", "tt");
      session.save();
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      Property prop = tNode.setProperty("t2", 1);
      assertEquals(PropertyType.STRING, prop.getType());
   }

   public void testReregisterRequiredNodeTypeChangeProperty() throws Exception
   {
      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRequiredNodeTypeChangeProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("tt", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.UNDEFINED, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", "exo:testReregisterRequiredNodeTypeChangeProperty");
      tNode.setProperty("tt", 1);

      session.save();

      // chenge mandatory
      List<PropertyDefinitionValue> props2 = new ArrayList<PropertyDefinitionValue>();
      props2.add(new PropertyDefinitionValue("tt", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props2);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }
      tNode.remove();
      session.save();

      tNode = root.addNode("test", "exo:testReregisterRequiredNodeTypeChangeProperty");
      tNode.setProperty("tt", "tt");
      session.save();
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      Property prop = tNode.setProperty("tt", "22");
      assertEquals(PropertyType.STRING, prop.getType());
   }

   public void testReregisterValueConstraintChangeResidualProperty() throws Exception
   {

      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterValueConstraintChangeResidualProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.LONG, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      Node tNode = root.addNode("test", "exo:testReregisterValueConstraintChangeResidualProperty");
      tNode.setProperty("tt", 100);
      Property prop = tNode.setProperty("t1", 150);
      tNode.setProperty("t2", 1);
      tNode.setProperty("t3", 200);
      session.save();
      List<String> valueConstraint = new ArrayList<String>();
      valueConstraint.add("(,100]");
      valueConstraint.add("[200,)");
      props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.LONG, valueConstraint));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok;
      }
      prop.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterValueConstraintChangeProperty() throws Exception
   {

      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterValueConstraintChangeProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("t1", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.LONG, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", "exo:testReregisterValueConstraintChangeProperty");

      Property prop = tNode.setProperty("t1", 150);
      session.save();
      List<String> valueConstraint = new ArrayList<String>();
      valueConstraint.add("(,100]");
      valueConstraint.add("[200,)");
      props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("t1", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.LONG, valueConstraint));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok;
      }

      tNode.setProperty("t1", 100);
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterIsMultipleChangeResidualProperty() throws Exception
   {

      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterIsMultipleChangeResidualProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), true,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node tNode = root.addNode("test", "exo:testReregisterIsMultipleChangeResidualProperty");
      Property prop = tNode.setProperty("t1", new String[]{"100", "150"});

      session.save();
      props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("*", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok;
      }
      prop.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      try
      {
         prop = tNode.setProperty("t1", new String[]{"100", "150"});
         session.save();
         fail();
      }
      catch (ValueFormatException e)
      {
         // ok
      }
   }

   public void testReregisterIsMultipleChangeProperty() throws Exception
   {

      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterIsMultipleChangeProperty");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      List<PropertyDefinitionValue> props = new ArrayList<PropertyDefinitionValue>();

      props.add(new PropertyDefinitionValue("t1", false, false, 1, false, new ArrayList<String>(), true,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node tNode = root.addNode("test", "exo:testReregisterIsMultipleChangeProperty");
      Property prop = tNode.setProperty("t1", new String[]{"100", "150"});

      session.save();
      props = new ArrayList<PropertyDefinitionValue>();
      props.add(new PropertyDefinitionValue("t1", false, false, 1, false, new ArrayList<String>(), false,
         PropertyType.STRING, new ArrayList<String>()));
      testNValue.setDeclaredPropertyDefinitionValues(props);
      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok;
      }
      prop.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      try
      {
         prop = tNode.setProperty("t1", new String[]{"100", "150"});
         session.save();
         fail();
      }
      catch (ValueFormatException e)
      {
         // ok
      }
   }

   /**
    * @throws Exception
    */
   public void testReregisterRemoveResidualChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRemoveResidualChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      nodes
         .add(new NodeDefinitionValue("*", false, false, 1, false, "nt:unstructured", new ArrayList<String>(), false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", "exo:testReregisterRemoveResidualChildNodeDefinition");
      Node child = testNode.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();

      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }
      child.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      try
      {
         child = testNode.addNode("child");
         session.save();
      }
      catch (ConstraintViolationException e)
      {
         // e.printStackTrace();
      }
   }

   // fail
   public void _testReregisterAddMixVersionable() throws Exception
   {

      // part1 any to string
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterAddMixVersionable");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();

      superType = new ArrayList<String>();
      superType.add("nt:base");
      superType.add("mix:versionable");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

   }

   /**
    * @throws Exception
    */
   public void testReregisterRemoveChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRemoveChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node testNode = root.addNode("testNode", testNValue.getName());
      Node child = testNode.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();

      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }
      child.remove();
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      try
      {
         child = testNode.addNode("child");
         session.save();
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
         // e.printStackTrace();
      }
   }

   /**
    * @throws Exception
    */
   public void testReregisterMandatoryNotAutocreatedChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterMandatoryNotAutocreatedChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, true, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

      testNode.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, true, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

   }

   /**
    * @throws Exception
    */
   public void testReregisterMandatoryChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterMandatoryChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node testNode = root.addNode("testNode", testNValue.getName());
      // testNode.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, true, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }

      testNode.addNode("child");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   /**
    * @throws Exception
    */
   public void testReregisterProtectedChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterProtectedChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      // testNode.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, true, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok;
      }

      testNode.addNode("child");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

   }

   public void testReregisterRequiredTypeChangeChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRequiredTypeChangeChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      List<String> requeredPrimaryType = new ArrayList<String>();
      requeredPrimaryType.add("nt:hierarchyNode");
      nodes
         .add(new NodeDefinitionValue("child", false, false, 1, false, "nt:hierarchyNode", requeredPrimaryType, false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());
      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();

      try
      {
         testNode.addNode("wrongchild", "nt:unstructured");
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }
      Node child = testNode.addNode("child", "nt:file");
      Node cont = child.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text");
      cont.setProperty("jcr:lastModified", new GregorianCalendar(2011, 3, 4));
      cont.setProperty("jcr:data", "test text");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      requeredPrimaryType = new ArrayList<String>();
      requeredPrimaryType.add("nt:folder");
      nodes
         .add(new NodeDefinitionValue("child", false, false, 1, false, "nt:hierarchyNode", requeredPrimaryType, false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {

         // ok
      }
      child.remove();
      session.save();

      child = testNode.addNode("child", "nt:folder");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterRequiredTypeChangeResidualChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterRequiredTypeChangeResidualChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();
      List<String> requeredPrimaryType = new ArrayList<String>();
      requeredPrimaryType.add("nt:base");
      nodes.add(new NodeDefinitionValue("*", false, false, 1, false, "nt:base", requeredPrimaryType, false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();

      Node child = testNode.addNode("child", "nt:file");
      Node cont = child.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text");
      cont.setProperty("jcr:lastModified", new GregorianCalendar(2011, 3, 4));
      cont.setProperty("jcr:data", "test text");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      requeredPrimaryType = new ArrayList<String>();
      requeredPrimaryType.add("nt:unstructured");
      nodes.add(new NodeDefinitionValue("*", false, false, 1, false, "nt:base", requeredPrimaryType, false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {

         // ok
      }
      child.remove();
      session.save();

      child = testNode.addNode("child", "nt:unstructured");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterisAllowsSameNameSiblingsChangeChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterisAllowsSameNameSiblingsChangeChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();

      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         true));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      Node child = testNode.addNode("child");
      Node child1 = child.addNode("child");
      Node child2 = child.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {

         // ok
      }
      child.remove();

      session.save();

      child = testNode.addNode("child");
      child1 = child.addNode("child");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterisAllowsSameNameSiblingsChangeResidualChildNodeDefinition() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterisAllowsSameNameSiblingsChangeResidualChildNodeDefinition");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();

      nodes.add(new NodeDefinitionValue("*", false, false, 1, false, "nt:unstructured", new ArrayList<String>(), true));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      Node child = testNode.addNode("child");
      Node child1 = child.addNode("child");
      Node child2 = child.addNode("child");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes
         .add(new NodeDefinitionValue("*", false, false, 1, false, "nt:unstructured", new ArrayList<String>(), false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {

         // ok
      }
      child.remove();

      session.save();

      child = testNode.addNode("child");
      child1 = child.addNode("child");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterFromNameToResidualChangeRequiredNodeType() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterFromNameToResidualChechRequiredNodeType");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();

      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:base", new ArrayList<String>(), true));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      Node child = testNode.addNode("child", "nt:file");
      Node cont = child.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text");
      cont.setProperty("jcr:lastModified", new GregorianCalendar(2011, 3, 4));
      cont.setProperty("jcr:data", "test text");
      session.save();
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      List<String> requeredPrimaryType = new ArrayList<String>();
      requeredPrimaryType.add("nt:unstructured");
      nodes.add(new NodeDefinitionValue("*", false, false, 1, false, "nt:unstructured", requeredPrimaryType, true));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
      child.remove();
      session.save();
      child = testNode.addNode("child", "nt:unstructured");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);

   }

   public void testReregisterFromNameToResidualChangeSameNameSibling() throws Exception
   {
      // create new NodeType value
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregister");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);

      List<NodeDefinitionValue> nodes = new ArrayList<NodeDefinitionValue>();

      nodes.add(new NodeDefinitionValue("child", false, false, 1, false, "nt:unstructured", new ArrayList<String>(),
         true));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      Node child = testNode.addNode("child");
      child.addNode("subchild1");
      child.addNode("subchild1");
      session.save();

      nodes = new ArrayList<NodeDefinitionValue>();
      nodes
         .add(new NodeDefinitionValue("*", false, false, 1, false, "nt:unstructured", new ArrayList<String>(), false));
      testNValue.setDeclaredChildNodeDefinitionValues(nodes);

      try
      {
         nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
      child.remove();
      session.save();

      child = testNode.addNode("child");
      child.addNode("subchild1");
      session.save();

      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
   }

   public void testReregisterIsMixinChange1() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterIsMixinChange1");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      testNValue.setMixin(false);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode", testNValue.getName());
      session.save();
      testNValue.setMixin(true);
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

      testNode = root.addNode("testNode");
      testNode.addMixin(testNValue.getName());
      session.save();
   }

   public void testReregisterIsMixinChange2() throws Exception
   {
      NodeTypeValue testNValue = new NodeTypeValue();

      List<String> superType = new ArrayList<String>();
      superType.add("nt:base");
      testNValue.setName("exo:testReregisterIsMixinChange2");
      testNValue.setPrimaryItemName("");
      testNValue.setDeclaredSupertypeNames(superType);
      testNValue.setMixin(true);
      nodeTypeManager.registerNodeType(testNValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
      testNValue = nodeTypeManager.getNodeTypeValue(testNValue.getName());

      Node testNode = root.addNode("testNode");
      testNode.addMixin(testNValue.getName());
      session.save();

      testNValue.setMixin(false);
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
   }

   public void testCyclicDependencies() throws Exception
   {
      List<NodeTypeValue> list = new ArrayList<NodeTypeValue>();
      NodeTypeValue testNValueA = new NodeTypeValue();
      testNValueA.setName("exo:testCyclicDependenciesA");
      NodeTypeValue testNValueB = new NodeTypeValue();
      testNValueB.setName("exo:testCyclicDependenciesB");

      List<String> superTypeA = new ArrayList<String>();
      superTypeA.add("nt:base");
      superTypeA.add(testNValueB.getName());

      testNValueA.setPrimaryItemName("");
      testNValueA.setDeclaredSupertypeNames(superTypeA);
      testNValueA.setMixin(false);

      List<String> superTypeB = new ArrayList<String>();
      superTypeB.add("nt:base");
      superTypeB.add(testNValueA.getName());

      testNValueB.setPrimaryItemName("");
      testNValueB.setDeclaredSupertypeNames(superTypeB);
      testNValueB.setMixin(false);

      list.add(testNValueA);
      list.add(testNValueB);

      nodeTypeManager.registerNodeTypes(list, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
   }

   public void testRandomSequenceDependencies() throws Exception
   {
      List<NodeTypeValue> list = new ArrayList<NodeTypeValue>();
      NodeTypeValue testNValueA = new NodeTypeValue();
      testNValueA.setName("exo:testRandomSequenceDependenciesA");
      NodeTypeValue testNValueB = new NodeTypeValue();
      testNValueB.setName("exo:testRandomSequenceDependenciesB");

      List<String> superTypeA = new ArrayList<String>();
      superTypeA.add("nt:base");
      superTypeA.add(testNValueB.getName());

      testNValueA.setPrimaryItemName("");
      testNValueA.setDeclaredSupertypeNames(superTypeA);
      testNValueA.setMixin(false);

      List<String> superTypeB = new ArrayList<String>();
      superTypeB.add("nt:base");

      testNValueB.setPrimaryItemName("");
      testNValueB.setDeclaredSupertypeNames(superTypeB);
      testNValueB.setMixin(false);

      list.add(testNValueA);
      list.add(testNValueB);

      nodeTypeManager.registerNodeTypes(list, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
   }

   public void testCyclicUnresolvedDependencies() throws Exception
   {
      List<NodeTypeValue> list = new ArrayList<NodeTypeValue>();
      NodeTypeValue testNValueA = new NodeTypeValue();
      testNValueA.setName("exo:testCyclicUnresolvedDependenciesA");
      NodeTypeValue testNValueB = new NodeTypeValue();
      testNValueB.setName("exo:testCyclicUnresolvedDependenciesB");

      List<String> superTypeA = new ArrayList<String>();
      superTypeA.add("nt:base");
      superTypeA.add(testNValueB.getName());

      testNValueA.setPrimaryItemName("");
      testNValueA.setDeclaredSupertypeNames(superTypeA);
      testNValueA.setMixin(false);

      List<String> superTypeB = new ArrayList<String>();
      superTypeB.add("nt:base");
      superTypeB.add("exo:testCyclicUnresolvedDependenciesC");

      testNValueB.setPrimaryItemName("");
      testNValueB.setDeclaredSupertypeNames(superTypeB);
      testNValueB.setMixin(false);

      list.add(testNValueA);
      list.add(testNValueB);

      try
      {
         nodeTypeManager.registerNodeTypes(list, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
         fail();
      }
      catch (RepositoryException e)
      {
         // e.printStackTrace();
         // ok
      }
   }

   /**
    * Test http://jira.exoplatform.org/browse/JCR-859
    * 
    * @throws Exception
    */
   public void testJCR859() throws Exception
   {

      InputStream xml =
         this.getClass().getResourceAsStream("/org/exoplatform/services/jcr/impl/core/nodetype/test-jcr589.xml");
      repositoryService.getCurrentRepository().getNodeTypeManager().registerNodeTypes(xml,
         ExtendedNodeTypeManager.FAIL_IF_EXISTS, NodeTypeDataManager.TEXT_XML);

      Node tr = root.addNode("testRoot");
      Node l1 = tr.addNode("t", "myNodeTypes");
      l1.addNode("l2", "myNodeType");
      l1.addNode("l3", "myNodeTypes");
      session.save();
   }
}
