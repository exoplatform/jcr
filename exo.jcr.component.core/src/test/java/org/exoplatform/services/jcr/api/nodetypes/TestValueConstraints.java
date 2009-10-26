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
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS.
 */

public class TestValueConstraints extends JcrAPIBaseTest
{

   private Node testValueConstraintsNode = null;

   private NodeTypeManagerImpl ntManager = null;

   private Node refNodeNtUnstructured = null;

   private String nodeTypeName = "jcr:testValueConstraints";

   private static String LOCAL_BIG_FILE = null;

   private static String LOCAL_SMALL_FILE = null;

   private static String LOCAL_NORMAL_FILE = null;

   public void setUp() throws Exception
   {
      super.setUp();

      LOCAL_SMALL_FILE = createBLOBTempFile(16).getAbsolutePath(); // 16Kb
      LOCAL_BIG_FILE = createBLOBTempFile(16 * 1024).getAbsolutePath(); // 16Mb
      LOCAL_NORMAL_FILE = createBLOBTempFile(17).getAbsolutePath(); // 17Kb

      byte[] xmlData = readXmlContent("/org/exoplatform/services/jcr/api/nodetypes/nodetypes-api-test.xml");
      ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlData);
      ntManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();
      ntManager.registerNodeTypes(xmlInput, 0, NodeTypeDataManager.TEXT_XML);
      assertNotNull(ntManager.getNodeType(nodeTypeName));
      Node ntRoot = (Node)repository.getSystemSession().getItem(NodeTypeManagerImpl.NODETYPES_ROOT);
      assertTrue(ntRoot.hasNode(nodeTypeName));
      testValueConstraintsNode = root.addNode("testValueConstraints", nodeTypeName);
      testValueConstraintsNode.addMixin("mix:referenceable");
      refNodeNtUnstructured = root.addNode("testref", "nt:unstructured");
      refNodeNtUnstructured.addMixin("mix:referenceable");
      session.save();
   }

   public void testSTRING1Property() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testSTRING1", "abc");
      session.save();
      try
      {
         testProperty.setValue("abcd");
         session.save();
         fail("setValue(STRING value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // success
      }
   }

   public void testSTRING2Property() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testSTRING2", "abc");
      testProperty.setValue("abcd");
      testProperty.setValue("");
      testProperty.setValue("1234");
      testProperty.setValue("true");
      session.save();
   }

   public void testPATHProperty() throws Exception
   {

      Property testProperty =
         testValueConstraintsNode.setProperty("jcr:testPATH", valueFactory.createValue("/abc", PropertyType.PATH));
      Value value = valueFactory.createValue("../exojcrtest:def/ghi", PropertyType.PATH);
      testProperty.setValue(value);
      session.save();
      try
      {
         testProperty.setValue(valueFactory.createValue("/abcd", PropertyType.PATH));
         session.save();
         fail("setValue(PATH value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // success
      }
      try
      {
         testProperty.setValue(valueFactory.createValue("../abc", PropertyType.PATH));
         session.save();
         fail("setValue(PATH value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // success
      }
   }

   public void testNAMEProperty() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testNAME", valueFactory.createValue("abc:"));
      testProperty.setValue(valueFactory.createValue("abc:def"));
      session.save();
      try
      {
         testProperty.setValue(valueFactory.createValue("/abcd"));
         session.save();
         fail("setValue(NAME value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // success
      }
      try
      {
         testProperty.setValue(valueFactory.createValue("abc:de"));
         session.save();
         fail("setValue(NAME value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // success
      }
   }

   public void testBINARYINCLUSIVEProperty() throws Exception
   {

      Property testProperty =
         testValueConstraintsNode.setProperty("jcr:testBINARYINCLUSIVE", new FileInputStream(LOCAL_SMALL_FILE));
      testProperty.setValue(new FileInputStream(LOCAL_BIG_FILE));
      session.save();
   }

   public void testBINARYEXCLUSIVEProperty() throws Exception
   {

      try
      {
         Property testProperty =
            testValueConstraintsNode.setProperty("jcr:testBINARYEXCLUSIVE", new FileInputStream(LOCAL_SMALL_FILE));
         testProperty.setValue(new FileInputStream(LOCAL_BIG_FILE));
         session.save();
         fail("setValue(BINARY value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
      // trying to use another file to get constraint viol.
      try
      {
         Property testProperty =
            testValueConstraintsNode.setProperty("jcr:testBINARYEXCLUSIVE", new FileInputStream(LOCAL_NORMAL_FILE));
         session.save();
         fail("setValue(BINARY value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
   }

   public void testDATEINCLUSIVEProperty() throws Exception
   {

      Property testProperty =
         testValueConstraintsNode.setProperty("jcr:testDATEINCLUSIVE", valueFactory.createValue(
            "1111-11-11T11:11:11.111Z", PropertyType.DATE));
      testProperty.setValue(valueFactory.createValue("1222-11-11T11:11:11.111Z", PropertyType.DATE));
      session.save();
   }

   public void testDATEEXCLUSIVEProperty() throws Exception
   {

      try
      {
         Property testProperty =
            testValueConstraintsNode.setProperty("jcr:testDATEEXCLUSIVE", valueFactory.createValue(
               "1111-11-11T11:11:11.111Z", PropertyType.DATE));
         testProperty.setValue(valueFactory.createValue("1222-11-11T11:11:11.111Z", PropertyType.DATE));
         session.save();
         fail("setValue(DATE value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
      // trying to use another date to get constr.viol.ex.
      try
      {
         Property testProperty =
            testValueConstraintsNode.setProperty("jcr:testDATEEXCLUSIVE", valueFactory.createValue(
               "1155-11-11T11:11:11.111Z", PropertyType.DATE));
         session.save();
         fail("setValue(DATE value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
   }

   public void testLONGINCLUSIVEProperty() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testLONGINCLUSIVE", 100);
      testProperty.setValue(200);
      session.save();
   }

   public void testLONGEXCLUSIVEProperty() throws Exception
   {

      try
      {
         Property testProperty = testValueConstraintsNode.setProperty("jcr:testLONGEXCLUSIVE", 100);
         testProperty.setValue(200);
         session.save();
         fail("setValue(LONG value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
      // trying to use another Long to get ex.
      try
      {
         Property testProperty = testValueConstraintsNode.setProperty("jcr:testLONGEXCLUSIVE", 150);
         session.save();
         fail("setValue(LONG value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
   }

   public void testDOUBLEINCLUSIVEProperty() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testDOUBLEINCLUSIVE", 100);
      testProperty.setValue(200);
      session.save();
   }

   public void testDOUBLEEXCLUSIVEProperty() throws Exception
   {

      try
      {
         Property testProperty = testValueConstraintsNode.setProperty("jcr:testDOUBLEEXCLUSIVE", 100);
         testProperty.setValue(200);
         session.save();
         fail("setValue(DOUBLE value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
      // trying to use another Double to get ex.
      try
      {
         Property testProperty = testValueConstraintsNode.setProperty("jcr:testDOUBLEEXCLUSIVE", 150);
         session.save();
         fail("setValue(DOUBLE value) must throw a ConstraintViolationException ");
      }
      catch (Exception e)
      {
         // succes
      }
   }

   public void testBOOLEANProperty() throws Exception
   {

      Property testProperty = testValueConstraintsNode.setProperty("jcr:testBOOLEAN", true);
      session.save();
      try
      {
         testProperty.setValue(false);
         root.save();
      }
      catch (Exception e)
      {
         fail("setValue(BOOLEAN value) here should be no Exception ");
      }
   }

   private byte[] readXmlContent(String fileName)
   {
      try
      {
         InputStream is = TestValueConstraints.class.getResourceAsStream(fileName);
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         int r = is.available();
         byte[] bs = new byte[r];
         while (r > 0)
         {
            r = is.read(bs);
            if (r > 0)
            {
               output.write(bs, 0, r);
            }
            r = is.available();
         }
         is.close();
         return output.toByteArray();
      }
      catch (Exception e)
      {
         log.error("Error read file '" + fileName + "' with NodeTypes. Error:" + e);
         return null;
      }
   }

   protected void tearDown() throws Exception
   {
      if (session.getRootNode().hasNode("testValueConstraints"))
      {
         session.getRootNode().getNode("testValueConstraints").remove();
      }
      if (session.getRootNode().hasNode("testref"))
      {
         session.getRootNode().getNode("testref").remove();
      }
      super.tearDown();
   }

}
