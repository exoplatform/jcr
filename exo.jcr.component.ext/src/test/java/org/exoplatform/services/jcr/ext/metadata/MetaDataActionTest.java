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
package org.exoplatform.services.jcr.ext.metadata;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;

import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

public class MetaDataActionTest extends BaseStandaloneTest
{

   /**
    * <value> <object type="org.exoplatform.services.jcr.impl.ext.action.ActionConfiguration"> <field
    * name="eventTypes"><string>addProperty,changeProperty</string></field> <field
    * name="path"><string>/MetaDataActionTest/testAddContent</string></field> <field
    * name="isDeep"><boolean>true</boolean></field> <field
    * name="parentNodeType"><string>nt:resource</string></field> <field
    * name="actionClassName"><string
    * >org.exoplatform.services.jcr.ext.metadata.AddMetadataAction</string></field> </object>
    * </value>
    * 
    * @throws Exception
    */
   public void testAddContent() throws Exception
   {

      InputStream is = MetaDataActionTest.class.getResourceAsStream("/test_index.xls");
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      session.save();
      Node contentNode = rootNode.addNode("testAddContent", "nt:resource");
      // contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/excel");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();

      Node testNode = repository.getSystemSession().getRootNode().getNode("MetaDataActionTest/testAddContent");
      assertTrue(testNode.hasProperty("dc:creator"));
      assertTrue(testNode.hasProperty("dc:date"));
      assertTrue(testNode.hasProperty("dc:contributor"));
   }

   /**
    * Prerequisites: <value> <object
    * type="org.exoplatform.services.jcr.impl.ext.action.ActionConfiguration"> <field
    * name="eventTypes"><string>addNode</string></field> <field
    * name="path"><string>/MetaDataActionTest/setmetadata</string></field> <field
    * name="isDeep"><boolean>false</boolean></field> <field
    * name="actionClassName"><string>org.exoplatform
    * .services.jcr.ext.metadata.SetDCMetadataAction</string></field> </object> </value>
    * 
    * 
    * @throws Exception
    */
   public void testSetMetaData() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      session.save();
      Node contentNode = rootNode.addNode("testSetMetaData");
      rootNode.save();
      assertTrue(contentNode.hasProperty("dc:creator"));
      assertTrue(contentNode.hasProperty("dc:date"));
      assertEquals(session.getUserID(), contentNode.getProperty("dc:creator").getValues()[0].getString());
   }

   public void testDontSetMetaData() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      session.save();
      Node contentNode = rootNode.addNode("testDontSetMetaData");
      contentNode.setProperty("prop", "prop 1");
      rootNode.save();
      assertFalse(contentNode.hasProperty("dc:creator"));
      assertFalse(contentNode.hasProperty("dc:date"));
      assertFalse(contentNode.hasProperty("dc:creator"));
   }

   public void testDontSetMetaDataNtFile() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      session.save();
      Node node = rootNode.addNode("testDontSetMetaDataNtFile", "nt:file");
      Node contentNode = node.addNode("jcr:content", "nt:unstructured");
      contentNode.setProperty("jcr:data", MetaDataActionTest.class.getResourceAsStream("/test_index.xls"));
      contentNode.setProperty("jcr:mimeType", "application/vnd.ms-excel");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      // dc:elementset properties SHOULD NOT be setted automatically
      rootNode.save();

      assertFalse(contentNode.hasProperty("dc:creator"));
      assertFalse(contentNode.hasProperty("dc:date"));
      assertFalse(contentNode.hasProperty("dc:creator"));
   }

   public void testDontSetMetaDataAnywhere() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      session.save();
      Node contentNode = session.getRootNode().addNode("testDontSetMetaDataAnywhere");
      contentNode.setProperty("prop", "prop 1");
      rootNode.save();
      assertFalse(contentNode.hasProperty("dc:creator"));
      assertFalse(contentNode.hasProperty("dc:date"));
      assertFalse(contentNode.hasProperty("dc:creator"));
   }
   
   public void _testUpdatePDF() throws Exception
   {
      InputStream is = MetaDataActionTest.class.getResourceAsStream("/test_1.pdf");

      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      Node contentNode = rootNode.addNode("testAddContent", "nt:resource");
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:mimeType", "application/pdf");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();

      Node testNode = repository.getSystemSession().getRootNode().getNode("MetaDataActionTest/testAddContent");
      assertTrue(testNode.hasProperty("dc:title"));
      assertTrue(testNode.hasProperty("dc:creator"));
      assertEquals("Title_1", testNode.getProperty("dc:title").getValues()[0].getString());
      assertEquals("Author_1", testNode.getProperty("dc:creator").getValues()[0].getString());

      // update #1 
      is = MetaDataActionTest.class.getResourceAsStream("/test_2.pdf");
      contentNode.setProperty("jcr:data", is);
      session.save();

      testNode = repository.getSystemSession().getRootNode().getNode("MetaDataActionTest/testAddContent");
      assertTrue(testNode.hasProperty("dc:title"));
      assertTrue(testNode.hasProperty("dc:creator"));
      assertEquals("Title_2", testNode.getProperty("dc:title").getValues()[0].getString());
      assertEquals("Author_2", testNode.getProperty("dc:creator").getValues()[0].getString());

      // update #2 
      is = MetaDataActionTest.class.getResourceAsStream("/test_3.pdf");
      contentNode.setProperty("jcr:data", is);
      session.save();

      testNode = repository.getSystemSession().getRootNode().getNode("MetaDataActionTest/testAddContent");
      assertFalse(testNode.hasProperty("dc:title"));
      assertFalse(testNode.hasProperty("dc:creator"));
   }

   public void testJcrSetPropertyTestCase() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      Node contentNode = rootNode.addNode("testAddContent", "nt:resource");
      contentNode.setProperty("jcr:mimeType", "");
      contentNode.setProperty("jcr:data", "");
      contentNode.setProperty("jcr:lastModified", new GregorianCalendar());
      session.save();

      //try set property
      contentNode.setProperty("jcr:mimeType", "image/jpeg");
      session.save();
   }

   /*
    * This test checking use-case like use-case describe in issue JCR-1873.
    * If we change content in node and this content has new mimeType then we have exception. 
    * And also We checking metadata.
    */
   public void testCheckMetaDataIfChangeMimeTypeAndData() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("MetaDataActionTest");
      Node nodeWithDOC = rootNode.addNode("testAddContent", "nt:resource");
      nodeWithDOC.setProperty("jcr:mimeType", "application/msword");
      nodeWithDOC.setProperty("jcr:data", MetaDataActionTest.class.getResourceAsStream("/testDOC.doc"));
      nodeWithDOC.setProperty("jcr:lastModified", Calendar.getInstance());

      Node nodeWithPDF = rootNode.addNode("testAddContent2", "nt:resource");
      nodeWithPDF.setProperty("jcr:mimeType", "application/pdf");
      nodeWithPDF.setProperty("jcr:data", MetaDataActionTest.class.getResourceAsStream("/testPDF.pdf"));
      nodeWithPDF.setProperty("jcr:lastModified", Calendar.getInstance());

      session.save();

      nodeWithDOC.setProperty("jcr:mimeType", "application/pdf");
      nodeWithDOC.setProperty("jcr:data", MetaDataActionTest.class.getResourceAsStream("/testPDF.pdf"));

      session.save();

      HashMap<String, Property> map = new HashMap<String, Property>();
      for (PropertyIterator props = nodeWithDOC.getProperties(); props.hasNext();)
      {
         Property prop = props.nextProperty();
         map.put(prop.getName(), prop);
      }

      evalProps(nodeWithPDF.getProperties(), map);
   }

   private void evalProps(PropertyIterator etalon, HashMap<String, Property> testedProps) throws RepositoryException
   {
      while (etalon.hasNext())
      {
         Property prop = etalon.nextProperty();
         String propertyName = prop.getName();
         if (propertyName.startsWith("dc:"))
         {
            Property testProperty = testedProps.get(propertyName);
            assertNotNull(propertyName + " property not founded. ", testProperty);
            assertEquals(propertyName + " property value is incorrect", testProperty.getValues()[0],
               prop.getValues()[0]);
         }
      }
   }

}
