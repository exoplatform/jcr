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
package org.exoplatform.services.jcr.api.importing;

import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.impl.util.StringConverter;
import org.exoplatform.services.jcr.impl.xml.importing.ContentImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestDocumentViewImport.java 14244 2008-05-14 11:44:54Z ksm $
 */
public class TestDocumentViewImport extends AbstractImportTest
{
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.TestDocumentViewImport");

   private final String docView =
      "<exo:test xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
         + "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" "
         + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" "
         + "xmlns:exo=\"http://www.exoplatform.com/jcr/exo/1.0\" "
         + "xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" "
         + "jcr:primaryType=\"nt:unstructured\">"
         + "<childNode jcr:created=\"2004-08-18T20:07:42.626+01:00\" jcr:primaryType=\"nt:folder\">"
         + "<childNode3 jcr:created=\"2004-08-18T20:07:42.636+01:00\" jcr:primaryType=\"nt:file\">"
         + "<jcr:content jcr:data=\"dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=\" jcr:primaryType=\"nt:resource\" jcr:lastModified=\"2004-08-18T20:07:42.626+01:00\" jcr:mimeType=\"text/html\" jcr:uuid=\"1092852462407_\">"
         + "</jcr:content>"
         + "</childNode3>"
         + "<childNode2 jcr:created=\"2004-08-18T20:07:42.636+01:00\" jcr:primaryType=\"nt:file\">"
         + "<jcr:content jcr:data=\"VGhyZWUgYnl0ZXMgYXJlIGNvbmNhdGVuYXRlZCwgdGhlbiBzcGxpdCB0byBmb3JtIDQgZ3JvdXBz"
         + "IG9mIDYtYml0cyBlYWNoOw==\" jcr:primaryType=\"nt:resource\" jcr:mimeType=\"text/html\" jcr:lastModified=\"2004-08-18T20:07:42.626+01:00\" jcr:uuid=\"1092852462406_\">"
         + "</jcr:content>"
         + "</childNode2>"
         + "</childNode>"
         + "<testNodeWithText1 jcr:mixinTypes='mix:referenceable' jcr:uuid='id_uuidNode3' testProperty='test property value'>Thisi is a text content of node &lt;testNodeWithText1/&gt; </testNodeWithText1>"
         + "<testNodeWithText2><![CDATA[This is a text content of node <testNodeWithText2>]]></testNodeWithText2>"
         + "<uuidNode1 jcr:mixinTypes='mix:referenceable' jcr:uuid='id_uuidNode1' source='docView'/>" + "</exo:test>";

   private final String docView2 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<childNode2 " + "xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
         + "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" " + "jcr:primaryType=\"nt:file\" "
         + "jcr:created=\"2004-08-18T17:17:00.856+03:00\">" + "<jcr:content " + "jcr:primaryType=\"nt:resource\" "
         + "jcr:uuid=\"6a3859dac0a8004b006e6e0bf444ebaa\" " + "jcr:data=\"dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=\" "
         + "jcr:lastModified=\"2004-08-18T17:17:00.856+03:00\" "
         + "jcr:lastModified2=\"2004-08-18T17:17:00.856+03:00\" " + "jcr:mimeType=\"text/text\"/>" + "</childNode2>";

   private final String docViewECM =
      "<test-article xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:kfx=\"http://www.exoplatform.com/jcr/kfx/1.1/\" xmlns:Fwd=\"http://www.exoplatform.com/jcr/Fwd/1.1/\" xmlns:Re=\"http://www.exoplatform.com/jcr/Re/1.1/\" xmlns:rma=\"http://www.rma.com/jcr/\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" xmlns:fn=\"http://www.w3.org/2004/10/xpath-functions\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:exo=\"http://www.exoplatform.com/jcr/exo/1.0\" exo:summary=\"\" exo:voteTotal=\"0\" exo:votingRate=\"0.0\" jcr:primaryType=\"exo:article\" jcr:mixinTypes=\"mix:votable mix:i18n\" jcr:uuid=\"6da3fcebc0a800070043d28761e00078\" exo:language=\"en\" exo:title=\"title\" exo:text=\"\"></test-article>";

   private final String NAV_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<node-navigation>" + "<owner-type>portal</owner-type>"
         + "<owner-id>portalone</owner-id>" + "<access-permissions>*:/guest</access-permissions>" + "<page-nodes>"
         + "<node>" + "<uri>portalone::home</uri>" + "<name>home</name>" + "<label>Home</label>"
         + "<page-reference>portal::portalone::content</page-reference>" + "</node>" + "<node>"
         + "<uri>portalone::register</uri>" + "<name>register</name>" + "<label>Register</label>"
         + "<page-reference>portal::portalone::register</page-reference>" + "</node>" + "</page-nodes>"
         + "</node-navigation>";

   private final String NAV_XML2 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         + "<node-navigation  xmlns:jcr='http://www.jcp.org/jcr/1.0' jcr:primaryType='nt:unstructured' >"
         + "<owner-type>portal</owner-type>" + "<owner-id>portalone</owner-id>"
         + "<access-permissions>*:/guest</access-permissions>" + "<page-nodes>" + "<node>"
         + "<uri>portalone::home</uri>" + "<name>home</name>" + "<label>Home</label>"
         + "<page-reference>portal::portalone::content</page-reference>" + "</node>" + "<node>"
         + "<uri>portalone::register</uri>" + "<name>register</name>" + "<label>Register</label>"
         + "<page-reference>portal::portalone::register</page-reference>" + "</node>" + "</page-nodes>"
         + "</node-navigation>";

   private final String xmlSameNameSablings4Xmltext = "<html><body>a<b>b</b>c</body></html>";

   private final String xmlSpeacialChars = "<html><body>a&lt;b>b&lt;/b>c</body></html>";

   private final Random random;

   public TestDocumentViewImport()
   {
      super();
      random = new Random();
   }

   public void testDocImportUnExistingPropertyDefinition() throws Exception
   {
      InvocationContext context = new InvocationContext();
      try
      {

         context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);
         ((ExtendedSession)session)
            .importXML(root.getPath(), new ByteArrayInputStream(docView2.getBytes()), 0, context);
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
      try
      {
         context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, false);
         ((ExtendedSession)session)
            .importXML(root.getPath(), new ByteArrayInputStream(docView2.getBytes()), 0, context);
         session.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
   }

   public void testDocViewImportContentHandler() throws Exception
   {

      XMLReader reader = XMLReaderFactory.createXMLReader();
      root.addNode("test");
      reader.setContentHandler(session.getImportContentHandler("/test", 0));
      InputSource inputSource = new InputSource(new ByteArrayInputStream(docView.getBytes()));

      reader.parse(inputSource);
      // fail ("STOP");
      session.save();

      Node root = session.getRootNode();
      NodeIterator iterator = root.getNode("test/exo:test").getNodes();
      assertEquals(4, iterator.getSize());

      iterator = root.getNode("test/exo:test/childNode").getNodes();
      assertEquals(2, iterator.getSize());

      Property property = root.getProperty("test/exo:test/childNode/childNode2/jcr:content/jcr:data");
      assertEquals("Three bytes are concatenated, then split to form 4 groups of 6-bits each;", property.getString());

      // property =
      // root.getProperty("childNode/childNode3/jcr:content/exo:content");
      // assertEquals("this is the binary content", property.getString());
   }

   public void testFindNodeType() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "nt:folder");
      try
      {
         deserialize(testRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            new ByteArrayInputStream(NAV_XML.getBytes()));
         testRoot.getSession().save();
         fail();
      }
      catch (ConstraintViolationException e)
      {
      }

      try
      {
         deserialize(testRoot, XmlSaveType.SESSION, false, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            new ByteArrayInputStream(NAV_XML.getBytes()));
         testRoot.getSession().save();
         fail();
      }
      catch (SAXException e)
      {
      }

   }

   public void testImportDocumentViewContentHandlerInvalidChildNodeType() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "nt:folder");

      Node exportRoot = root.addNode("exportRoot", "exo:article");

      exportRoot.setProperty("exo:title", "title");
      exportRoot.setProperty("exo:text", "text");

      session.save();

      byte[] content = serialize(exportRoot, false, true);

      try
      {
         deserialize(testRoot, XmlSaveType.SESSION, false, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            new ByteArrayInputStream(content));
         testRoot.getSession().save();
         fail();
      }
      catch (SAXException e)
      {
      }
   }

   public void testImportDocumentViewStreamInvalidChildNodeType() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "nt:folder");

      Node exportRoot = root.addNode("exportRoot", "exo:article");

      exportRoot.setProperty("exo:title", "title");
      exportRoot.setProperty("exo:text", "text");

      session.save();

      byte[] content = serialize(exportRoot, false, true);

      try
      {
         deserialize(testRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            new ByteArrayInputStream(content));
         testRoot.getSession().save();
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testImportDocView() throws RepositoryException, InvalidSerializedDataException,
      ConstraintViolationException, IOException, ItemExistsException
   {
      root.addNode("test2");
      session.importXML("/test2", new ByteArrayInputStream(docView.getBytes()), 0);
      session.save();

      Node root = session.getRootNode().getNode("test2");
      NodeIterator iterator = root.getNodes();

      assertEquals(1, iterator.getSize());
      // log.debug(">>"+session.getWorkspaceDataContainer()); iterator =
      iterator = root.getNode("exo:test/childNode").getNodes();
      assertEquals(2, iterator.getSize());
      Property property = root.getProperty("exo:test/childNode/childNode3/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());
      property = root.getProperty("exo:test/childNode/childNode2/jcr:content/jcr:data");
      assertEquals("Three bytes are concatenated, then split to form 4 groups of 6-bits each;", property.getString());

   }

   public void testImportDocViewECM() throws RepositoryException, InvalidSerializedDataException,
      ConstraintViolationException, IOException, ItemExistsException
   {
      root.addNode("testECM");
      session.importXML("/testECM", new ByteArrayInputStream(docViewECM.getBytes()), 0);
      session.save();

      Node testEcmNode = session.getRootNode().getNode("testECM");

      NodeIterator iterator = testEcmNode.getNodes();
      assertEquals(1, iterator.getSize());

      Node nodeArticle = root.getNode("testECM/test-article");
      assertEquals("title", nodeArticle.getProperty("exo:title").getString());
   }

   public void testImportRawXml() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "exo:registryGroup");
      session.save();

      deserialize(testRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
         new ByteArrayInputStream(NAV_XML.getBytes()));
      testRoot.getSession().save();

      Node node_navigation = testRoot.getNode("node-navigation");

      assertTrue(node_navigation.isNodeType("exo:registryEntry"));
      Node owner_type = node_navigation.getNode("owner-type");
      assertTrue(owner_type.isNodeType("nt:unstructured"));
   }

   public void testImportRawXmlFail() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "exo:registryGroup");
      session.save();

      try
      {
         deserialize(testRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
            new ByteArrayInputStream(NAV_XML2.getBytes()));
         testRoot.getSession().save();
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }

   }

   public void testImportVersionableFile() throws Exception
   {
      Node testPdf = root.addNode("testPdf", "nt:file");
      Node contentTestPdfNode = testPdf.addNode("jcr:content", "nt:resource");
      try
      {
         File file = createBLOBTempFile(25);// 2.5M
         log.info("=== File has created, size " + file.length());
         contentTestPdfNode.setProperty("jcr:data", new FileInputStream(file));
         contentTestPdfNode.setProperty("jcr:mimeType", "application/octet-stream");
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      contentTestPdfNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      session.save();
      testPdf.addMixin("mix:versionable");
      session.save();
      testPdf.checkin();
      testPdf.checkout();
      testPdf.checkin();
      byte[] buf = serialize(testPdf, false, true);

      Node importRoot = root.addNode("ImportRoot");

      deserialize(importRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
         new ByteArrayInputStream(buf));

      session.save();

      assertTrue(importRoot.hasNode("testPdf"));
      Node testRoot2 = importRoot.getNode("testPdf");
      assertTrue(testRoot2.isNodeType("mix:versionable"));

   }

   public void testImportXmlCh() throws Exception
   {

      XMLReader reader = XMLReaderFactory.createXMLReader();

      reader.setContentHandler(session.getImportContentHandler(root.getPath(), 0));
      InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlSpeacialChars.getBytes()));

      reader.parse(inputSource);

      session.save();
      Node htmlNode = root.getNode("html");
      Node bodyNode = htmlNode.getNode("body");
      Node xmlTextNode = bodyNode.getNode("jcr:xmltext");
      String xmlChars = xmlTextNode.getProperty("jcr:xmlcharacters").getString();
      assertTrue(StringConverter.denormalizeString(xmlSpeacialChars).contains(xmlChars));
   }

   public void testImportXmlChSameNameSablings() throws Exception
   {

      XMLReader reader = XMLReaderFactory.createXMLReader();

      reader.setContentHandler(session.getImportContentHandler(root.getPath(), 0));
      InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlSameNameSablings4Xmltext.getBytes()));

      reader.parse(inputSource);

      session.save();
      Node htmlNode = root.getNode("html");
      Node bodyNode = htmlNode.getNode("body");
      NodeIterator xmlTextNodes = bodyNode.getNodes("jcr:xmltext");
      assertEquals(2, xmlTextNodes.getSize());

   }

   public void testImportXmlStream() throws Exception
   {

      session.importXML(root.getPath(), new ByteArrayInputStream(xmlSpeacialChars.getBytes()),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
      session.save();
      Node htmlNode = root.getNode("html");
      Node bodyNode = htmlNode.getNode("body");
      Node xmlTextNode = bodyNode.getNode("jcr:xmltext");
      String xmlChars = xmlTextNode.getProperty("jcr:xmlcharacters").getString();
      assertTrue(StringConverter.denormalizeString(xmlSpeacialChars).contains(xmlChars));
   }

   public void testImportXmlStreamSameNameSablings() throws Exception
   {

      session.importXML(root.getPath(), new ByteArrayInputStream(xmlSameNameSablings4Xmltext.getBytes()),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
      session.save();
      Node htmlNode = root.getNode("html");
      Node bodyNode = htmlNode.getNode("body");
      NodeIterator xmlTextNodes = bodyNode.getNodes("jcr:xmltext");
      assertEquals(2, xmlTextNodes.getSize());
      Node nodeA = xmlTextNodes.nextNode();
      Node nodeC = xmlTextNodes.nextNode();
      assertEquals("a", nodeA.getProperty("jcr:xmlcharacters").getString());
      assertEquals("c", nodeC.getProperty("jcr:xmlcharacters").getString());
   }
}
