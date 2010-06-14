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
import org.exoplatform.services.jcr.impl.util.StringConverter;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: TestExportSysView.java 11962 2008-03-16 16:31:14Z gazarenkov $
 */

public class TestExportSysView extends ExportBase
{

   public TestExportSysView() throws ParserConfigurationException
   {
      super();

   }

   @Override
   public void initRepository() throws RepositoryException
   {
      Node rootNode = session.getRootNode();
      Node file = rootNode.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");

      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

   }

   @Override
   public void tearDown() throws Exception
   {
      Node rootNode = session.getRootNode();
      rootNode.getNode("childNode").remove();
      session.save();

      super.tearDown();
   }

   public void testExportCHNamespaceRemaping() throws Exception
   {

      Session newSession = repository.login(this.credentials /* session.getCredentials() */);
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

      SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
      TransformerHandler handler = saxFact.newTransformerHandler();
      handler.setResult(new StreamResult(bos));

      newSession.exportSystemView(testNode.getPath(), handler, false, false);

      bos.close();
      String exportContent = bos.toString();
      assertFalse(exportContent.contains("newjcr"));
      newSession.logout();
   }

   public void testExportPdf() throws RepositoryException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Node testPdf = root.addNode("testPdf", "nt:file");
      Node contentTestPdfNode = testPdf.addNode("jcr:content", "nt:resource");
      try
      {
         File file = createBLOBTempFile(2500);// 2.5M
         log.info("=== File has created, size " + file.length());
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
         log.info("===Starting export...");
         session.exportDocumentView("/testPdf", out, false, false);
         log.info("===Export has finished successfully");
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

   public void testExportReferenceableNodes() throws Exception
   {
      Node testNode = root.addNode("refNode");
      testNode.addMixin("mix:referenceable");
      session.save();
      File destFile = PrivilegedFileHelper.createTempFile("testExportReferenceableNodes", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);
      session.exportSystemView(testNode.getPath(), outStream, false, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));
      NodeList nodes = doc.getElementsByTagName("sv:node");
      assertEquals(1, nodes.getLength());
      assertEquals(3, nodes.item(0).getChildNodes().getLength());
      assertEquals("jcr:uuid", nodes.item(0).getChildNodes().item(2).getAttributes().getNamedItem("sv:name")
         .getNodeValue());
      assertEquals(testNode.getUUID(), nodes.item(0).getChildNodes().item(2).getChildNodes().item(0).getTextContent());
   }

   public void testExportStreamNamespaceRemaping() throws Exception
   {

      Session newSession = repository.login(this.credentials /* session.getCredentials() */);

      newSession.setNamespacePrefix("newjcr", "http://www.jcp.org/jcr/1.0");

      Node testNode = newSession.getRootNode().addNode("jcr:testExportNamespaceRemaping");
      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      newSession.save();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      newSession.exportSystemView(testNode.getPath(), bos, false, false);
      bos.close();
      String exportContent = bos.toString();
      assertFalse(exportContent.contains("newjcr"));

      newSession.logout();
   }

   public void testLockNodeExport() throws Exception
   {
      Node firstNode = root.addNode("forExport");
      Node testNode = firstNode.addNode("sysLockNode");
      testNode.addMixin("mix:lockable");
      session.save();
      testNode.lock(true, true);

      File destFile = PrivilegedFileHelper.createTempFile("sysLockNodeExport", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);

      session.exportSystemView(firstNode.getPath(), outStream, false, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      // assertEquals(Constants.DEFAULT_ENCODING, doc.getXmlEncoding());

      // NodeList list = doc.getElementsByTagName("sysLockNode");
      NodeList list = doc.getElementsByTagName("sv:node");
      // 2 nodes exported
      assertEquals(2, list.getLength());
      // 2 properties primariType and mixinType
      assertEquals(2, list.item(1).getChildNodes().getLength());
      testNode.unlock();
      testNode.remove();
      session.save();
      assertTrue(destFile.delete());
   }

   public void testMultyValueExportCh() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException, IOException, SAXException,
      XPathExpressionException, TransformerConfigurationException
   {
      Node testNode = root.addNode("syschMultyValueExportStream");

      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      session.save();
      File destFile = PrivilegedFileHelper.createTempFile("xmlTest", ".xml");
      OutputStream outputStream2 = PrivilegedFileHelper.fileOutputStream(destFile);

      SAXTransformerFactory saxFact = (SAXTransformerFactory)TransformerFactory.newInstance();
      TransformerHandler handler = saxFact.newTransformerHandler();
      handler.setResult(new StreamResult(outputStream2));

      long startTime = System.currentTimeMillis();
      session.exportSystemView(testNode.getPath(), handler, false, false);
      outputStream2.close();
      log.info("Export with handler done " + (System.currentTimeMillis() - startTime) / 1000 + " sec");

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      XPathExpression expr = xpath.compile("//sv:property");
      Object result = expr.evaluate(doc, XPathConstants.NODESET);
      // System.out.println(result);
      NodeList nodes = (NodeList)result;
      for (int i = 1; i < nodes.getLength(); i++)
      {
         String propertyName = nodes.item(i).getAttributes().getNamedItem("sv:name").getNodeValue();
         StringTokenizer tokenizer = new StringTokenizer(propertyName, "_");
         tokenizer.nextToken();

         String[] pureValues = valList.get(Integer.parseInt(tokenizer.nextToken()));
         String type = tokenizer.nextToken();

         NodeList nodeValues = nodes.item(i).getChildNodes();

         assertEquals(pureValues.length, nodeValues.getLength());

         for (int j = 0; j < nodeValues.getLength(); j++)
         {
            String exportedContent = nodeValues.item(j).getTextContent();
            if ("string".equals(type))
            {
               assertEquals(pureValues[j], StringConverter.denormalizeString(exportedContent));
            }
            else if ("binary".equals(type))
            {
               assertEquals(pureValues[j], new String(Base64.decode(exportedContent), Constants.DEFAULT_ENCODING));
            }
         }
      }
      destFile.delete();
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
      File destFile = PrivilegedFileHelper.createTempFile("sysMultyValueExportStream", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);
      session.exportSystemView(testNode.getPath(), outStream, false, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      XPathExpression expr = xpath.compile("//sv:property");

      Object result = expr.evaluate(doc, XPathConstants.NODESET);
      // System.out.println(result);
      NodeList nodes = (NodeList)result;

      for (int i = 1; i < nodes.getLength(); i++)
      {
         String propertyName = nodes.item(i).getAttributes().getNamedItem("sv:name").getNodeValue();
         StringTokenizer tokenizer = new StringTokenizer(propertyName, "_");
         tokenizer.nextToken();
         String[] pureValues = valList.get(Integer.parseInt(tokenizer.nextToken()));
         String type = tokenizer.nextToken();
         NodeList nodeValues = nodes.item(i).getChildNodes();

         assertEquals(pureValues.length, nodeValues.getLength());

         for (int j = 0; j < nodeValues.getLength(); j++)
         {
            String exportedContent = nodeValues.item(j).getTextContent();
            if ("string".equals(type))
            {
               assertEquals(pureValues[j], StringConverter.denormalizeString(exportedContent));
            }
            else if ("binary".equals(type))
            {
               assertEquals(pureValues[j], new String(Base64.decode(exportedContent), Constants.DEFAULT_ENCODING));
            }
         }
      }
      destFile.delete();
   }

   public void testMultyValueExportStreamSkipBinary() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException, IOException, SAXException,
      XPathExpressionException
   {
      Node testNode = root.addNode("MultyValueExportStream");

      for (int i = 0; i < valList.size(); i++)
      {
         testNode.setProperty("prop_" + i + "_string", valList.get(i), PropertyType.STRING);
         testNode.setProperty("prop_" + i + "_binary", valList.get(i), PropertyType.BINARY);
      }

      session.save();
      File destFile = PrivilegedFileHelper.createTempFile("multyValueExportStreamSkipBinary", ".xml");
      PrivilegedFileHelper.deleteOnExit(destFile);
      OutputStream outStream = PrivilegedFileHelper.fileOutputStream(destFile);
      session.exportSystemView(testNode.getPath(), outStream, true, false);
      outStream.close();

      Document doc = builder.parse(PrivilegedFileHelper.fileInputStream(destFile));

      XPathExpression expr = xpath.compile("//sv:property");
      Object result = expr.evaluate(doc, XPathConstants.NODESET);
      // System.out.println(result);
      NodeList nodes = (NodeList)result;
      for (int i = 1; i < nodes.getLength(); i++)
      {
         String propertyName = nodes.item(i).getAttributes().getNamedItem("sv:name").getNodeValue();
         StringTokenizer tokenizer = new StringTokenizer(propertyName, "_");
         tokenizer.nextToken();
         String[] pureValues = valList.get(Integer.parseInt(tokenizer.nextToken()));
         String type = tokenizer.nextToken();
         NodeList nodeValues = nodes.item(i).getChildNodes();

         assertEquals(pureValues.length, nodeValues.getLength());

         for (int j = 0; j < nodeValues.getLength(); j++)
         {
            String exportedContent = nodeValues.item(j).getTextContent();
            if ("string".equals(type))
            {
               assertEquals(pureValues[j], StringConverter.denormalizeString(exportedContent));
            }
            else if ("binary".equals(type))
            {
               assertEquals("", exportedContent);
            }
         }
      }
      destFile.delete();
   }

   public void testWithContentHandler() throws RepositoryException, SAXException
   {
      MockContentHandler mock = new MockContentHandler();
      session.exportSystemView("/childNode", mock, false, true);
      assertEquals(1, mock.nodes);
      assertEquals(2, mock.properties);
   }
}
