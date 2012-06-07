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
package org.exoplatform.services.jcr.api.reading;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.PropertyInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestProperty.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestProperty extends JcrAPIBaseTest
{

   private Node node;

   public void initRepository() throws RepositoryException
   {
      Node root = session.getRootNode();
      node = root.addNode("childNode", "nt:unstructured");

      Value[] values = new Value[3];
      values[0] = valueFactory.createValue("stringValue");
      values[1] = valueFactory.createValue("true");
      values[2] = valueFactory.createValue("121");
      node.setProperty("multi", values, PropertyType.STRING);
      node.setProperty("multi-boolean", new Value[]{session.getValueFactory().createValue(true),
         session.getValueFactory().createValue(true)});

      node.setProperty("single", session.getValueFactory().createValue("this is the content"));

      ByteArrayInputStream is = new ByteArrayInputStream("streamValue".getBytes());
      node.setProperty("stream", valueFactory.createValue(is));

   }

   public void tearDown() throws Exception
   {
      node.remove();

      super.tearDown();
   }

   public void testGetValue() throws RepositoryException
   {

      Property property = node.getProperty("single");
      assertTrue(property.getValue() instanceof Value);

      try
      {
         node.getProperty("multi").getValue();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }

      try
      {
         node.getProperty("multi-boolean").getBoolean();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }

   }

   public void testGetValues() throws RepositoryException
   {
      Value[] values = node.getProperty("multi").getValues();
      for (int i = 0; i < values.length; i++)
      {
         Value value = values[i];
         if (!("stringValue".equals(value.getString()) || "true".equals(value.getString()) || "121".equals(value
            .getString())))
         {
            fail("returned non expected value");
         }
      }
      try
      {
         node.getProperty("single").getValues();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetString() throws RepositoryException
   {
      node.setProperty("string", session.getValueFactory().createValue("stringValue"));

      String stringValue = node.getProperty("string").getString();
      assertEquals("stringValue", stringValue);

      try
      {
         node.getProperty("multi").getString();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetBinaryAsStream() throws RepositoryException, IOException
   {

      node.setProperty("stream",
         new BinaryValue(new ByteArrayInputStream("inputStream".getBytes()), SpoolConfig.getDefaultSpoolConfig()));
      Value value = node.getProperty("stream").getValue();
      InputStream iS = value.getStream();
      byte[] bytes = new byte[iS.available()];
      iS.read(bytes);
      assertEquals("inputStream", new String(bytes));
      try
      {
         value.getString();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      iS.reset();
      iS = node.getProperty("stream").getValue().getStream();
      bytes = new byte[iS.available()];
      iS.read(bytes);
      assertEquals("inputStream", new String(bytes));

   }

   public void testGetLong() throws RepositoryException
   {
      node.setProperty("long", valueFactory.createValue(15l));
      assertEquals(15, node.getProperty("long").getLong());
      assertEquals(15, node.getProperty("long").getValue().getLong());

      node.setProperty("noLong", "someText");
      try
      {
         node.getProperty("noLong").getLong();
         fail();
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetDouble() throws RepositoryException
   {
      node.setProperty("double", session.getValueFactory().createValue(15));
      assertEquals(15, (int)node.getProperty("double").getDouble());
      assertEquals(15, (int)node.getProperty("double").getValue().getDouble());

      try
      {
         node.getProperty("multi").getDouble();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }

      node.setProperty("noDouble", "someText");
      try
      {
         node.getProperty("noDouble").getDouble();
         fail();
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetDate() throws RepositoryException
   {

      Calendar calendar = new GregorianCalendar();
      node.setProperty("date", session.getValueFactory().createValue(calendar));
      assertEquals(calendar.getTimeInMillis(), node.getProperty("date").getDate().getTimeInMillis());
      assertEquals(calendar.getTimeInMillis(), node.getProperty("date").getValue().getDate().getTimeInMillis());

      node.setProperty("noDate", "someText");
      try
      {
         node.getProperty("noDate").getDate();
         fail();
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetBoolean() throws RepositoryException
   {
      node.setProperty("boolean", session.getValueFactory().createValue(true));
      assertEquals(true, node.getProperty("boolean").getBoolean());
      assertEquals(true, node.getProperty("boolean").getValue().getBoolean());
   }

   public void testGetLength() throws RepositoryException, IOException
   {
      Property property = node.getProperty("single");
      assertTrue(property.getLength() > 0);
      property = node.getProperty("stream");
      // node.setProperty("stream", new BinaryValue(new
      // ByteArrayInputStream("inputStream".getBytes())));
      Value b = valueFactory.createValue(new ByteArrayInputStream("inputStream".getBytes()));
      property.setValue(b);

      assertTrue(property.getLength() > 0);

      try
      {
         node.getProperty("multi").getLength();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetLengths() throws RepositoryException, IOException
   {
      Property property = node.getProperty("multi");
      assertTrue(property.getLengths()[0] > 0);

      try
      {
         node.getProperty("single").getLengths();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
   }

   public void testGetDefinition() throws RepositoryException
   {
      Property property = node.getProperty("single");
      assertEquals("*", property.getDefinition().getName());
   }

   public void testGetType() throws RepositoryException
   {
      assertEquals(PropertyType.STRING, node.getProperty("single").getType());
      assertEquals(PropertyType.STRING, node.getProperty("multi").getType());
   }

   public void testGetBinaryAsString() throws RepositoryException, IOException
   {

      // System.out.println("STREAM>>>>>>");

      node.setProperty("stream",
         new BinaryValue(new ByteArrayInputStream("inputStream".getBytes()), SpoolConfig.getDefaultSpoolConfig()));
      // System.out.println("STREAM>>>>>>");

      // log.debug("STREAM>>>>>>");
      Value value = node.getProperty("stream").getValue();
      assertEquals("inputStream", value.getString());
      try
      {
         value.getStream();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }

   }

   public void testGetNode() throws RepositoryException
   {
      Node root = session.getRootNode();
      Node node1 = root.addNode("childNode1", "nt:unstructured");

      Node refNode = node1.addNode("refNode", "nt:resource");
      refNode
         .setProperty("jcr:data", session.getValueFactory().createValue("this is the content", PropertyType.BINARY));
      refNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      refNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      Value refVal = valueFactory.createValue(refNode);
      Property p = node1.setProperty("reference", refVal);
      // log.debug("RefVal >>>"+p.getString());

      root.save();

      assertEquals(refNode.getUUID(), node1.getProperty("reference").getString());
      assertEquals(refNode.getPath(), node1.getProperty("reference").getNode().getPath());

      refNode.remove();

      node1.setProperty("noNode", "someText");
      try
      {
         node1.getProperty("noNode").getNode();
         fail();
      }
      catch (ValueFormatException e)
      {
      }
      finally
      {
         node1.remove();
      }
   }

   public void testEquals() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      Node testNode = root.addNode("testNode");
      PropertyImpl testProperty = (PropertyImpl)testNode.setProperty("testProperty", "someText");
      assertFalse(testProperty.equals(new Object()));

   }

   public void testPropertyInfoGetValuesSize()
   {
      PropertyInfo propertInfo = new PropertyInfo();

      assertEquals(0, propertInfo.getValuesSize());
   }
}