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
package org.exoplatform.services.jcr.api.exporting;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestExportImport.java 13888 2008-05-05 13:47:27Z ksm $
 */
public class TestExportImport extends ExportBase
{
   private final int SNS_NODES_COUNT = 10;

   public TestExportImport() throws ParserConfigurationException
   {
      super();
   }

   public void testExportImportCustomNodeType() throws Exception
   {
      Node folder = root.addNode("childNode", "nt:folder");
      Node file = folder.addNode("childNode2", "exo:salestool");

      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      File destFile = File.createTempFile("testExportImportValuesSysView", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);
      session.exportSystemView(file.getPath(), outStream, false, false);
      outStream.close();

      folder.remove();
      session.save();

      session.importXML(root.getPath(), new FileInputStream(destFile), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

   }

   public void testExportImportValuesSysView() throws Exception
   {
      Node testNode = root.addNode("testExportImport");
      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }
      session.save();
      File destFile = File.createTempFile("testExportImportValuesSysView", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);
      session.exportSystemView(testNode.getPath(), outStream, false, false);
      outStream.close();

      testNode.remove();
      session.save();

      session.importXML(root.getPath(), new FileInputStream(destFile), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

      Node newNode = root.getNode("testExportImport");

      for (int i = 0; i < valList.size(); i++)
      {
         if (valList.get(i).length > 1)
         {
            Value[] stringValues = newNode.getProperty("prop" + i + "_string").getValues();
            for (int j = 0; j < stringValues.length; j++)
            {
               assertEquals(stringValues[j].getString(), valList.get(i)[j]);
            }
            Value[] binaryValues = newNode.getProperty("prop" + i + "_binary").getValues();
            for (int j = 0; j < stringValues.length; j++)
            {
               assertEquals(binaryValues[j].getString(), valList.get(i)[j]);
            }
         }
         else
         {
            assertEquals(valList.get(i)[0], newNode.getProperty("prop" + i + "_string").getValue().getString());
            assertEquals(valList.get(i)[0], newNode.getProperty("prop" + i + "_binary").getValue().getString());

         }
      }
      destFile.delete();
   }

   public void testMixinExportImportDocumentViewContentHandler() throws Exception
   {

      Node testNode = root.addNode("childNode");
      testNode.setProperty("a", 1);
      testNode.addMixin("mix:versionable");
      testNode.addMixin("mix:lockable");

      session.save();
      doVersionTests(testNode);

      session.save();

      doExportImport(root, "childNode", false, true, null);

      session.save();

      Node childNode = root.getNode("childNode");
      doVersionTests(childNode);
   }

   public void testMixinExportImportDocumentViewStream() throws Exception
   {

      Node testNode = root.addNode("childNode");
      testNode.setProperty("a", 1);
      testNode.addMixin("mix:versionable");
      testNode.addMixin("mix:lockable");

      session.save();
      doVersionTests(testNode);

      session.save();

      doExportImport(root, "childNode", false, false, null);

      session.save();

      Node childNode = root.getNode("childNode");
      doVersionTests(childNode);
   }

   public void testMixinExportImportSystemViewContentHandler() throws Exception
   {

      Node testNode = root.addNode("childNode");
      testNode.setProperty("a", 1);
      testNode.addMixin("mix:versionable");
      testNode.addMixin("mix:lockable");

      session.save();
      doVersionTests(testNode);

      doExportImport(root, "childNode", true, true, null);

      Node childNode = root.getNode("childNode");
      doVersionTests(childNode);
   }

   public void testMixinExportImportSystemViewStream() throws Exception
   {

      Node testNode = root.addNode("childNode");
      testNode.setProperty("a", 1);
      testNode.addMixin("mix:versionable");
      testNode.addMixin("mix:lockable");

      session.save();
      doVersionTests(testNode);

      doExportImport(root, "childNode", true, false, null);

      Node childNode = root.getNode("childNode");
      doVersionTests(childNode);
   }

   public void testSNSDocumentViewCh() throws Exception
   {
      Node testSNS = root.addNode("testSNS");
      testSNS.addMixin("mix:versionable");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
      }

      Node testDest = root.addNode("testDest");
      session.save();

      doExportImport(root, "testSNS", false, true, testDest);
      assertTrue(testDest.hasNode("testSNS"));
      Node testSNSNew = testDest.getNode("testSNS");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
         assertTrue(testSNSNew.hasNode("nodeSNS[" + (i + 1) + "]"));
      }

   }

   public void testSNSDocumentViewStream() throws Exception
   {
      Node testSNS = root.addNode("testSNS");
      testSNS.addMixin("mix:versionable");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
      }

      Node testDest = root.addNode("testDest");
      session.save();

      doExportImport(root, "testSNS", false, false, testDest);
      assertTrue(testDest.hasNode("testSNS"));
      Node testSNSNew = testDest.getNode("testSNS");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
         assertTrue(testSNSNew.hasNode("nodeSNS[" + (i + 1) + "]"));
      }

   }

   public void testSNSSystemViewCh() throws Exception
   {
      Node testSNS = root.addNode("testSNS");
      testSNS.addMixin("mix:versionable");
      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
      }
      Node testDest = root.addNode("testDest");
      session.save();

      doExportImport(root, "testSNS", true, true, testDest);
      assertTrue(testDest.hasNode("testSNS"));
      Node testSNSNew = testDest.getNode("testSNS");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
         assertTrue(testSNSNew.hasNode("nodeSNS[" + (i + 1) + "]"));
      }
   }

   public void testSNSSystemViewStream() throws Exception
   {
      Node testSNS = root.addNode("testSNS");
      testSNS.addMixin("mix:versionable");
      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
      }
      Node testDest = root.addNode("testDest");
      session.save();

      doExportImport(root, "testSNS", true, false, testDest);
      assertTrue(testDest.hasNode("testSNS"));
      Node testSNSNew = testDest.getNode("testSNS");

      for (int i = 0; i < SNS_NODES_COUNT; i++)
      {
         testSNS.addNode("nodeSNS");
         assertTrue(testSNSNew.hasNode("nodeSNS[" + (i + 1) + "]"));
      }

   }

   private void doExportImport(Node parentNode, String nodeName, boolean isSystemView, boolean isContentHandler,
      Node destParentNode) throws RepositoryException, IOException, TransformerConfigurationException, SAXException
   {
      Node exportNode = parentNode.getNode(nodeName);
      File destFile = File.createTempFile("testExportImport", ".xml");
      destFile.deleteOnExit();
      OutputStream outStream = new FileOutputStream(destFile);

      if (isSystemView)
      {
         if (isContentHandler)
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            session.exportSystemView(exportNode.getPath(), handler, false, false);
         }
         else
         {
            session.exportSystemView(exportNode.getPath(), outStream, false, false);
         }
      }
      else
      {
         if (isContentHandler)
         {
            SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = saxFact.newTransformerHandler();
            handler.setResult(new StreamResult(outStream));
            session.exportDocumentView(exportNode.getPath(), handler, false, false);
         }
         else
         {
            session.exportDocumentView(exportNode.getPath(), outStream, false, false);
         }
      }

      outStream.close();
      if (destParentNode == null)
      {
         exportNode.remove();
         session.save();
      }

      session.importXML(destParentNode != null ? destParentNode.getPath() : root.getPath(), new FileInputStream(
         destFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

      session.save();
      assertTrue(parentNode.hasNode(nodeName));

   }

   public void testname() throws Exception
   {

   }

   private void doVersionTests(Node testNode) throws RepositoryException
   {
      assertTrue(testNode.isNodeType("mix:lockable"));
      assertTrue(testNode.isNodeType("mix:referenceable"));
      assertTrue(testNode.hasProperty("jcr:uuid"));
      assertTrue(testNode.isCheckedOut());
      testNode.setProperty("a", 1);

      session.save();
      Version version1 = testNode.checkin();
      testNode.checkout();
      testNode.setProperty("a", 2);

      session.save();
      assertEquals(2, testNode.getProperty("a").getLong());
      Version version2 = testNode.checkin();
      testNode.checkout();
      Property prop2 = testNode.getProperty("jcr:mixinTypes");
      assertEquals(PropertyType.NAME, prop2.getType());

      assertTrue(testNode.isCheckedOut());
      testNode.restore(version1, true);
      testNode.checkout();
      assertTrue(testNode.isCheckedOut());
      assertEquals(1, testNode.getProperty("a").getLong());

      Property prop = testNode.getProperty("jcr:mixinTypes");
      assertEquals(PropertyType.NAME, prop.getType());

      assertTrue(testNode.isNodeType("mix:lockable"));
      assertTrue(testNode.isNodeType("mix:referenceable"));
      assertTrue(testNode.hasProperty("jcr:uuid"));
      assertTrue(testNode.isCheckedOut());
   }

   public void testShouldThrowExceptionWhenExistingNodeAddedAfterImporting() throws Exception
   {
      Node testNode = root.addNode("testNode");
      testNode.addMixin("mix:referenceable");
      TransientNodeData node = (TransientNodeData)((NodeImpl)testNode.addNode("test2")).getData();
      session.save();

      File contentFile = new File("target/input-sysview.xml");
      OutputStream outStream = new FileOutputStream(contentFile);
      try
      {
         session.exportSystemView(testNode.getPath(), outStream, false, false);
      }
      finally
      {
         outStream.close();
      }

      testNode.remove();
      session.save();

      session.importXML(root.getPath(), new FileInputStream(contentFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();

      TransientNodeData newNodeData =
         new TransientNodeData(node.getQPath(), IdGenerator.generate(), node.getPersistedVersion(),
            node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), root.getNode("testNode")
               .getUUID(), node.getACL());

      ItemState state = ItemState.createAddedState(newNodeData);
      session.getTransientNodesManager().updateItemState(state);

      try
      {
         session.save();
         fail();
      }
      catch (Exception e)
      {
      }
   }

   public void testShouldThrowExceptionWhenExistingPropertyAddedAfterImporting() throws Exception
   {
      Node testNode = root.addNode("testNode");
      testNode.addMixin("mix:referenceable");
      TransientPropertyData prop =
         (TransientPropertyData)((PropertyImpl)testNode.setProperty("testProperty", "testValue")).getData();
      session.save();

      File contentFile = new File("target/input-sysview.xml");
      OutputStream outStream = new FileOutputStream(contentFile);
      try
      {
         session.exportSystemView(testNode.getPath(), outStream, false, false);
      }
      finally
      {
         outStream.close();
      }

      testNode.remove();
      session.save();

      session.importXML(root.getPath(), new FileInputStream(contentFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();

      TransientPropertyData newPropertyData =
         new TransientPropertyData(prop.getQPath(), IdGenerator.generate(), prop.getPersistedVersion(), prop.getType(),
            root.getNode("testNode").getUUID(), prop.isMultiValued(), prop.getValues());

      ItemState state = ItemState.createAddedState(newPropertyData);
      session.getTransientNodesManager().updateItemState(state);

      try
      {
         session.save();
         fail();
      }
      catch (Exception e)
      {
      }
   }

   public void testExportImportInvalidCharSystem() throws Exception
   {
      Node testNode = root.addNode("testExportImportInvalidCharSystem");
      String contentWithInvalidChar = "\001Af\001A\001A\001Ao\001A\001Ao\001A";
      String contentWoInvalidChar = "foo";
      testNode.setProperty("stringValue", contentWithInvalidChar);
      testNode.setProperty("binaryValue",
         new ByteArrayInputStream(contentWithInvalidChar.getBytes(Constants.DEFAULT_ENCODING)));
      testNode.setProperty("stringValueWo", contentWoInvalidChar);
      testNode.setProperty("binaryValueWo",
         new ByteArrayInputStream(contentWoInvalidChar.getBytes(Constants.DEFAULT_ENCODING)));
      session.save();
      assertEquals(PropertyType.STRING, testNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, testNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, testNode.getProperty("binaryValue").getType());
      assertEquals(contentWithInvalidChar, testNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, testNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, testNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, testNode.getProperty("binaryValueWo").getType());
      assertEquals(contentWoInvalidChar, testNode.getProperty("binaryValueWo").getString());

      // With Binary
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      session.exportSystemView(testNode.getPath(), outStream, false, false);
      outStream.close();

      testNode.remove();
      session.save();

      session.importXML(root.getPath(), new ByteArrayInputStream(outStream.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

      Node newNode = root.getNode("testExportImportInvalidCharSystem");
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("binaryValueWo").getString());

      outStream.reset();

      // Without Binary
      session.exportSystemView(newNode.getPath(), outStream, true, false);
      outStream.close();

      newNode.remove();
      root.save();

      session.importXML(root.getPath(), new ByteArrayInputStream(outStream.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

      newNode = root.getNode("testExportImportInvalidCharSystem");
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValue").getType());
      assertEquals("", newNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValueWo").getType());
      assertEquals("", newNode.getProperty("binaryValueWo").getString());
   }

   public void testExportImportInvalidCharSystemWithCH() throws Exception
   {
      Node testNode = root.addNode("testExportImportInvalidCharSystemWithCH");
      String contentWithInvalidChar = "\001Af\001A\001A\001Ao\001A\001Ao\001A";
      String contentWoInvalidChar = "foo";
      testNode.setProperty("stringValue", contentWithInvalidChar);
      testNode.setProperty("binaryValue",
         new ByteArrayInputStream(contentWithInvalidChar.getBytes(Constants.DEFAULT_ENCODING)));
      testNode.setProperty("stringValueWo", contentWoInvalidChar);
      testNode.setProperty("binaryValueWo",
         new ByteArrayInputStream(contentWoInvalidChar.getBytes(Constants.DEFAULT_ENCODING)));
      session.save();
      assertEquals(PropertyType.STRING, testNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, testNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, testNode.getProperty("binaryValue").getType());
      assertEquals(contentWithInvalidChar, testNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, testNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, testNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, testNode.getProperty("binaryValueWo").getType());
      assertEquals(contentWoInvalidChar, testNode.getProperty("binaryValueWo").getString());

      // With Binary
      final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      ContentHandler handler = new DefaultHandler()
      {
         private Map<String, String> prefixMapping = new LinkedHashMap<String, String>();
         private boolean start;
         public void startPrefixMapping (String prefix, String uri) throws SAXException
         {
            prefixMapping.put(prefix, uri);
         }
         
         public void startElement (String uri, String localName,
            String qName, Attributes atts) throws SAXException
         {
            try
            {
               outStream.write(("<" + qName).getBytes(Constants.DEFAULT_ENCODING));
               if (start)
               {
                  start = false;
                  for (String prefix : prefixMapping.keySet())
                  {
                     outStream.write((" xmlns:" + prefix + "=\"" + prefixMapping.get(prefix) + "\"").getBytes(Constants.DEFAULT_ENCODING));
                  }
               }
               for (int i = 0; i < atts.getLength(); i++)
               {
                  outStream.write((" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"").getBytes(Constants.DEFAULT_ENCODING));
               }
               outStream.write(">".getBytes(Constants.DEFAULT_ENCODING));
            }
            catch (Exception e)
            {
               throw new SAXException(e);
            }
         }
         
         public void startDocument() throws SAXException
         {
            try
            {
               outStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(Constants.DEFAULT_ENCODING));
               start = true;
            }
            catch (Exception e)
            {
               throw new SAXException(e);
            }
         }
         
         public void endElement(String uri, String localName,
            String qName) throws SAXException
         {
            try
            {
               outStream.write(("</" + qName + ">").getBytes(Constants.DEFAULT_ENCODING));
            }
            catch (Exception e)
            {
               throw new SAXException(e);
            }
         }
         
         public void endDocument() throws SAXException
         {
            prefixMapping.clear();
            start = false;
         }
         
         public void characters(char ch[], int start, int length) throws SAXException
         {
            try
            {
               outStream.write(new String (ch, start, length).getBytes(Constants.DEFAULT_ENCODING));
            }
            catch (Exception e)
            {
               throw new SAXException(e);
            }
         }
      };
      session.exportSystemView(testNode.getPath(), handler, false, false);
      outStream.close();

      testNode.remove();
      session.save();

      session.importXML(root.getPath(), new ByteArrayInputStream(outStream.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

      Node newNode = root.getNode("testExportImportInvalidCharSystemWithCH");
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("binaryValueWo").getString());

      outStream.reset();

      // Without Binary
      session.exportSystemView(newNode.getPath(), handler, true, false);
      outStream.close();

      newNode.remove();
      root.save();

      session.importXML(root.getPath(), new ByteArrayInputStream(outStream.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

      session.save();

      newNode = root.getNode("testExportImportInvalidCharSystemWithCH");
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValue").getType());
      assertEquals(contentWithInvalidChar, newNode.getProperty("stringValue").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValue").getType());
      assertEquals("", newNode.getProperty("binaryValue").getString());
      assertEquals(PropertyType.STRING, newNode.getProperty("stringValueWo").getType());
      assertEquals(contentWoInvalidChar, newNode.getProperty("stringValueWo").getString());
      assertEquals(PropertyType.BINARY, newNode.getProperty("binaryValueWo").getType());
      assertEquals("", newNode.getProperty("binaryValueWo").getString());
   }
}
