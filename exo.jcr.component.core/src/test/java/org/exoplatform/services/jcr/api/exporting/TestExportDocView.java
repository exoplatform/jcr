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

import org.apache.ws.commons.util.Base64;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;
import org.exoplatform.services.jcr.impl.util.StringConverter;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.StringTokenizer;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: TestExportDocView.java 11962 2008-03-16 16:31:14Z gazarenkov $
 */

public class TestExportDocView extends ExportBase
{

   public TestExportDocView() throws ParserConfigurationException
   {
      super();
   }

   @Override
   public void initRepository() throws RepositoryException
   {

      Node root = session.getRootNode();
      Node file = root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");

      Node contentNode = file.addNode("jcr:content", "nt:resource");
      try
      {
         Value value = new BinaryValue("this is the content");
         contentNode.setProperty("jcr:data", value);
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      log.debug(">> save childNode START");
      session.save();
      log.debug(">> save childNode END");

   }

   @Override
   public void tearDown() throws Exception
   {
      log.debug(">> get rootNode on TD START");

      Node root = session.getRootNode();
      log.debug(">> get childNode on TD START");
      // session.getItem("/childNode");
      root.getNode("childNode").remove();
      log.debug(">> get childNode on TD END ");

      session.save();

      super.tearDown();
   }

   public void testRus() throws Exception
   {
      Node nodeRus = root.addNode("testRus");
      String val = "\u043c\u0430\u043c\u0430 \u043c\u044b\u043b\u0430 \u0440\u0430\u043c\u0443.";
      nodeRus.setProperty("p1", val);
      session.save();
      assertEquals(val, nodeRus.getProperty("p1").getString());
   }

   public void testWithContentHandler() throws RepositoryException, SAXException
   {

      MockContentHandler mock = new MockContentHandler();

      mock = new MockContentHandler();
      session.exportDocumentView("/childNode", mock, false, true);
      assertEquals(1, mock.docElement);
   }

   public void testExportPdf() throws RepositoryException, IOException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      // export Xml problem
      Node testPdf = root.addNode("testPdf", "nt:file");
      Node contentTestPdfNode = testPdf.addNode("jcr:content", "nt:resource");
      try
      {
         File file = createBLOBTempFile(2500);// 2.5M
         if (log.isDebugEnabled())
            log.debug("=== File has created, size " + file.length());
         contentTestPdfNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(file));
         contentTestPdfNode.setProperty("jcr:mimeType", "application/octet-stream");
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      contentTestPdfNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      session.save();
      try
      {
         if (log.isDebugEnabled())
            log.debug("===Starting export...");
         session.exportDocumentView("/testPdf", out, false, false);
         if (log.isDebugEnabled())
            log.debug("===Export has finished successfully");
      }
      catch (Exception e)
      {
         e.printStackTrace();
         fail("Impossible to export pdf");
      }
      finally
      {
         testPdf.remove();
         session.save();
      }
   }

   public void testMultyValueExportStream() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException, SAXException,
      XPathExpressionException
   {
      Node testNode = root.addNode("MultyValueExportStream");

      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      session.save();
      File destFile = PrivilegedFileHelper.createTempFile("multyValueExportStream", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);
      session.exportDocumentView(testNode.getPath(), outStream, false, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      // assertEquals(Constants.DEFAULT_ENCODING, doc.getXmlEncoding());

      NodeList list = doc.getElementsByTagName("MultyValueExportStream");

      assertEquals(1, list.getLength());

      org.w3c.dom.Node domNode = list.item(0);
      NamedNodeMap attr = domNode.getAttributes();
      for (int i = 0; i < attr.getLength(); i++)
      {
         org.w3c.dom.Node attribute = attr.item(i);
         if ("jcr:primaryType".equals(attribute.getNodeName()))
         {
            assertEquals("nt:unstructured", attribute.getNodeValue());
         }
         else if (attribute.getNodeName().startsWith("prop"))
         {

            String propertyName = attribute.getNodeName();
            StringTokenizer tokenizer = new StringTokenizer(propertyName, "_");
            tokenizer.nextToken();
            String[] pureValues = valList.get(Integer.parseInt(tokenizer.nextToken()));
            String type = tokenizer.nextToken();

            String attrValue = attribute.getNodeValue();
            StringTokenizer spaceTokenizer = new StringTokenizer(attrValue);
            if (pureValues.length == 1 && pureValues[0].equals(""))
               assertEquals("", attrValue);
            else
               assertEquals(pureValues.length, spaceTokenizer.countTokens());
            int index = 0;
            while (spaceTokenizer.hasMoreTokens())
            {
               String exportedContent = spaceTokenizer.nextToken();
               if ("string".equals(type))
               {
                  assertEquals(pureValues[index], StringConverter.denormalizeString(exportedContent));
               }
               else if ("binary".equals(type))
               {
                  assertEquals(pureValues[index],
                     new String(Base64.decode(exportedContent), Constants.DEFAULT_ENCODING));

               }
               index++;
            }
         }
      }
   }

   public void testMultyValueExportCH() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException, SAXException,
      TransformerConfigurationException
   {
      Node testNode = root.addNode("MultyValueExportStream");

      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      session.save();
      File destFile = PrivilegedFileHelper.createTempFile("multyValueExportStream", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);

      SAXTransformerFactory saxFact = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
      TransformerHandler handler = saxFact.newTransformerHandler();
      handler.setResult(new StreamResult(outStream));

      try
      {
         session.exportDocumentView(testNode.getPath(), handler, false, false);
      }
      catch (RepositoryException e)
      {
      }
      finally
      {
         outStream.close();
      }

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      // assertEquals(Constants.DEFAULT_ENCODING, doc.getXmlEncoding());

      NodeList list = doc.getElementsByTagName("MultyValueExportStream");
      assertEquals(1, list.getLength());

      org.w3c.dom.Node domNode = list.item(0);
      NamedNodeMap attr = domNode.getAttributes();
      for (int i = 0; i < attr.getLength(); i++)
      {
         org.w3c.dom.Node attribute = attr.item(i);
         if ("jcr:primaryType".equals(attribute.getNodeName()))
         {
            assertEquals("nt:unstructured", attribute.getNodeValue());
         }
         else if (attribute.getNodeName().startsWith("prop"))
         {
            String propertyName = attribute.getNodeName();
            StringTokenizer tokenizer = new StringTokenizer(propertyName, "_");
            tokenizer.nextToken();
            String[] pureValues = valList.get(Integer.parseInt(tokenizer.nextToken()));
            String type = tokenizer.nextToken();

            String attrValue = attribute.getNodeValue();
            if (pureValues.length == 1 && pureValues[0].equals(""))
               assertEquals("", attrValue);
            else
            {
               StringTokenizer spaceTokenizer = new StringTokenizer(attrValue);
               assertEquals(pureValues.length, spaceTokenizer.countTokens());
               int index = 0;
               while (spaceTokenizer.hasMoreTokens())
               {
                  String exportedContent = spaceTokenizer.nextToken();
                  if ("string".equals(type))
                  {
                     assertEquals(pureValues[index], StringConverter.denormalizeString(exportedContent));
                  }
                  else if ("binary".equals(type))
                  {
                     assertEquals(pureValues[index], new String(Base64.decode(exportedContent),
                        Constants.DEFAULT_ENCODING));

                  }
                  index++;
               }
            }
         }
      }
   }

   public void testLockNodeExport() throws Exception
   {
      Node firstNode = root.addNode("forExport");
      Node testNode = firstNode.addNode("docLockNode");
      testNode.addMixin("mix:lockable");
      session.save();
      testNode.lock(true, true);

      File destFile = PrivilegedFileHelper.createTempFile("docLockNodeExport", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);

      session.exportDocumentView(firstNode.getPath(), outStream, false, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      // assertEquals(Constants.DEFAULT_ENCODING, doc.getXmlEncoding());

      NodeList list = doc.getElementsByTagName("docLockNode");
      assertEquals(1, list.getLength());
      // 2 properties primariType and mixinType
      assertEquals(2, list.item(0).getAttributes().getLength());
   }

   public void testExportStreamNamespaceRemaping() throws Exception
   {

      Session newSession = repository.login(this.credentials /*
                                                                                                    * session.getCredentials
                                                                                                    * ()
                                                                                                    */);

      newSession.setNamespacePrefix("newjcr", "http://www.jcp.org/jcr/1.0");

      Node testNode = newSession.getRootNode().addNode("jcr:testExportNamespaceRemaping");
      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      newSession.save();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      newSession.exportDocumentView(testNode.getPath(), bos, false, false);
      bos.close();
      String exportContent = bos.toString();
      assertFalse(exportContent.contains("newjcr"));

      newSession.logout();
   }

   public void testExportCHNamespaceRemaping() throws Exception
   {

      Session newSession = repository.login(this.credentials /*
                                                                                                    * session.getCredentials
                                                                                                    * ()
                                                                                                    */);
      newSession.setNamespacePrefix("newjcr", "http://www.jcp.org/jcr/1.0");

      Node testNode = newSession.getRootNode().addNode("jcr:testExportNamespaceRemaping");
      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      newSession.save();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // session.exportDocumentView(testNode.getPath(), bos, false, false);

      SAXTransformerFactory saxFact = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
      final TransformerHandler handler = saxFact.newTransformerHandler();
      handler.setResult(new StreamResult(bos));

      newSession.exportDocumentView(testNode.getPath(), handler, false, false);

      bos.close();
      String exportContent = bos.toString();
      assertFalse(exportContent.contains("newjcr"));
      newSession.logout();
   }
}
