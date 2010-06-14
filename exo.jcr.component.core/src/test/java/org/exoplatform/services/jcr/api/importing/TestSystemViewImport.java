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
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestSystemViewImport.java 14244 2008-05-14 11:44:54Z ksm $
 */
public class TestSystemViewImport extends AbstractImportTest
{
   static public final String SOURCE_NAME = "source node";

   static protected final String BIN_STRING = "222222222222222222<=Any binary=>22222222222222222222";

   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.TestSystemViewImport");

   private Node sysview;

   private File xmlContent;

   public static final String SYSTEM_VIEW_CONTENT =
      "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
         + "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" "
         + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" "
         + "xmlns:exo=\"http://www.exoplatform.com/jcr/exo/1.0\" "
         + "xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" sv:name=\"exo:test\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property>"
         +

         "<sv:node sv:name=\"childNode\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:folder</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "<sv:node sv:name=\"childNode3\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:file</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "<sv:node sv:name=\"jcr:content\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:resource</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\"><sv:value>1092835020617_</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:data\" sv:type=\"Binary\"><sv:value>dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:mimeType\" sv:type=\"String\"><sv:value>application/unknown</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:lastModified\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "</sv:node>"
         + "</sv:node>"
         + "<sv:node sv:name=\"childNode2\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:file</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "<sv:node sv:name=\"jcr:content\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:resource</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\"><sv:value>1092835020616_</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:data\" sv:type=\"Binary\"><sv:value>dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:mimeType\" sv:type=\"String\"><sv:value>text/text</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:lastModified\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "</sv:node>"
         + "</sv:node>"
         + "</sv:node>"
         +

         "<sv:node sv:name='uuidNode1'>"
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'><sv:value>nt:unstructured</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'>"
         + "<sv:value>mix:referenceable</sv:value>"
         + "<!-- sv:value>exo:accessControllable</sv:value -->"
         + "</sv:property>"
         + "<sv:property sv:name='jcr:test' sv:type='String'><sv:value>val1</sv:value><sv:value>val1</sv:value></sv:property>"
         + "<sv:property sv:name='source' sv:type='String'><sv:value>sysView</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:uuid' sv:type='String'><sv:value>id_uuidNode1</sv:value></sv:property>"
         + "</sv:node>"
         +

         "<sv:node sv:name='uuidNode2'>"
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'><sv:value>nt:unstructured</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'><sv:value>mix:referenceable</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:test' sv:type='String'><sv:value>val2</sv:value><sv:value>val1</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:uuid' sv:type='String'><sv:value>uuidNode2</sv:value></sv:property>"
         + "<sv:property sv:name='ref_to_1' sv:type='Reference'><sv:value>id_uuidNode1</sv:value></sv:property>"
         + "<sv:property sv:name='ref_to_1_and_3' sv:type='Reference'><sv:value>id_uuidNode1</sv:value><sv:value>id_uuidNode3</sv:value></sv:property>"
         + "<sv:property sv:name='ref_to_3' sv:type='Reference'><sv:value>id_uuidNode3</sv:value></sv:property>"
         + "</sv:node>"
         +

         "<sv:node sv:name='uuidNode3'>"
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'><sv:value>nt:unstructured</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'><sv:value>mix:referenceable</sv:value></sv:property>"
         + "<sv:property sv:name='ref_to_1' sv:type='Reference'><sv:value>id_uuidNode1</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:test' sv:type='String'><sv:value>val1</sv:value><sv:value>va31</sv:value></sv:property>"
         + "<sv:property sv:name='jcr:uuid' sv:type='String'><sv:value>id_uuidNode3</sv:value></sv:property>"
         + "</sv:node>"
         +

         "<sv:node sv:name=\"childNode4\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:test\" sv:type=\"String\"><sv:value>val1</sv:value><sv:value>val1</sv:value></sv:property>"
         + "</sv:node>" +

         "</sv:node>";

   public static final String SYSTEM_VIEW_CONTENT_FORMATTED =
      "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n "
         + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:exo=\"http://www.exoplatform.com/jcr/exo/1.0\"\n  "
         + "xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" sv:name=\"exo:test\">\n "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n      <sv:value>nt:unstructured</sv:value>\n    </sv:property>\n\n   "
         + "<sv:node sv:name=\"childNode\">\n      "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n         <sv:value>nt:folder</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\">\n       <sv:value>2004-08-18T15:17:00.856+01:00</sv:value>\n     </sv:property>\n     "
         + "<sv:node sv:name=\"childNode3\">\n        "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n            <sv:value>nt:file</sv:value>\n         </sv:property>\n        "
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\">\n          <sv:value>2004-08-18T15:17:00.856+01:00</sv:value>\n        </sv:property>\n        "
         + "<sv:node sv:name=\"jcr:content\">\n          "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n               <sv:value>nt:resource</sv:value>\n           </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">\n              <sv:value>1092835020617_</sv:value>\n           </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:data\" sv:type=\"Binary\">\n              <sv:value>dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=</sv:value>\n          </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:mimeType\" sv:type=\"String\">\n             <sv:value>application/unknown</sv:value>\n            </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:lastModified\" sv:type=\"Date\">\n              <sv:value>2004-08-18T15:17:00.856+01:00</sv:value>\n           </sv:property>\n        "
         + "</sv:node>\n      "
         + "</sv:node>\n      "
         + "<sv:node sv:name=\"childNode2\">\n        "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n            <sv:value>nt:file</sv:value>\n         </sv:property>\n        "
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\">\n          <sv:value>2004-08-18T15:17:00.856+01:00</sv:value>\n        </sv:property>\n        "
         + "<sv:node sv:name=\"jcr:content\">\n          "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n               <sv:value>nt:resource</sv:value>\n           </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">\n              <sv:value>1092835020616_</sv:value>\n           </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:data\" sv:type=\"Binary\">\n              <sv:value>dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=</sv:value>\n          </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:mimeType\" sv:type=\"String\">\n             <sv:value>text/text</sv:value>\n          </sv:property>\n           "
         + "<sv:property sv:name=\"jcr:lastModified\" sv:type=\"Date\">\n              <sv:value>2004-08-18T15:17:00.856+01:00</sv:value>\n           </sv:property>\n        "
         + "</sv:node>\n      "
         + "</sv:node>\n   "
         + "</sv:node>\n\n "
         + "<sv:node sv:name='uuidNode1'>\n     "
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'>\n       <sv:value>nt:unstructured</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'>\n        <sv:value>mix:referenceable</sv:value>\n        "
         + "<!-- sv:value>exo:accessControllable</sv:value -->\n     "
         + "</sv:property>\n     "
         + "<sv:property sv:name='jcr:test' sv:type='String'>\n         <sv:value>val1</sv:value>\n         <sv:value>val1</sv:value>\n      </sv:property>\n     "
         + "<sv:property sv:name='source' sv:type='String'>\n        <sv:value>sysView</sv:value>\n      </sv:property>\n     "
         + "<sv:property sv:name='jcr:uuid' sv:type='String'>\n         <sv:value>id_uuidNode1</sv:value>\n    </sv:property>\n  </sv:node>\n\n "
         + "<sv:node sv:name='uuidNode2'>\n     "
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'>\n       <sv:value>nt:unstructured</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'>\n        <sv:value>mix:referenceable</sv:value>\n     </sv:property>\n     "
         + "<sv:property sv:name='jcr:test' sv:type='String'>\n         <sv:value>val2</sv:value>\n         <sv:value>val1</sv:value>\n      </sv:property>\n     "
         + "<sv:property sv:name='jcr:uuid' sv:type='String'>\n         <sv:value>uuidNode2</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='ref_to_1' sv:type='Reference'>\n         <sv:value>id_uuidNode1</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='ref_to_1_and_3' sv:type='Reference'>\n         <sv:value>id_uuidNode1</sv:value>\n       <sv:value>id_uuidNode3</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='ref_to_3' sv:type='Reference'>\n         <sv:value>id_uuidNode3</sv:value>\n    </sv:property>\n  </sv:node>\n\n <sv:node sv:name='uuidNode3'>\n     "
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name'>\n       <sv:value>nt:unstructured</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='jcr:mixinTypes' sv:type='Name'>\n        <sv:value>mix:referenceable</sv:value>\n     </sv:property>\n     "
         + "<sv:property sv:name='ref_to_1' sv:type='Reference'>\n         <sv:value>id_uuidNode1</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name='jcr:test' sv:type='String'>\n         <sv:value>val1</sv:value>\n         <sv:value>va31</sv:value>\n      </sv:property>\n     <sv:property sv:name='jcr:uuid' sv:type='String'>\n         <sv:value>id_uuidNode3</sv:value>\n    </sv:property>\n  "
         + "</sv:node>\n\n "
         + "<sv:node sv:name=\"childNode4\">\n     "
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n         "
         + "<sv:value>nt:unstructured</sv:value>\n    </sv:property>\n     "
         + "<sv:property sv:name=\"jcr:test\" sv:type=\"String\">\n        <sv:value>val1</sv:value>\n         <sv:value>val1</sv:value>\n      </sv:property>\n  "
         + "</sv:node>\n\n" + "</sv:node>";

   public static final String SYSTEM_VIEW_CONTENT2 =
      "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
         + "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" "
         + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" "
         + "xmlns:exo=\"http://www.exoplatform.com/jcr/exo/1.0\" "
         + "xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" sv:name=\"childNode2\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:file</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:created\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "<sv:node sv:name=\"jcr:content\">"
         + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:resource</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\"><sv:value>1092835020616_</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:data\" sv:type=\"Binary\"><sv:value>dGhpcyBpcyB0aGUgYmluYXJ5IGNvbnRlbnQ=</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:mimeType\" sv:type=\"String\"><sv:value>text/text</sv:value></sv:property>"
         + "<sv:property sv:name=\"jcr:lastModified\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         // Special unexisting
         // property
         + "<sv:property sv:name=\"jcr:lastModified2\" sv:type=\"Date\"><sv:value>2004-08-18T15:17:00.856+01:00</sv:value></sv:property>"
         + "</sv:node>" + "</sv:node>";

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      sysview = session.getRootNode().addNode("test sysview", "nt:unstructured");

      Node ref = sysview.addNode(SOURCE_NAME, "nt:file");
      Node content = ref.addNode("jcr:content", "nt:unstructured");
      content.setProperty("anyDate", Calendar.getInstance());
      content.setProperty("anyString", "11111111111111<=Any string=>11111111111111111");
      content.setProperty("anyNumb", 123.321d);

      content.setProperty("anyBinary", BIN_STRING, PropertyType.BINARY);

      content.addNode("anyNode1").setProperty("_some_double", 1234.4321d);
      content.addNode("anyNode2").setProperty("_some_long", 123456789L);

      session.save();

      if (ref.canAddMixin("mix:referenceable"))
      {
         ref.addMixin("mix:referenceable");
         ref.save();
      }
      else
      {
         fail("Can't add mixin mix:referenceable");
      }

      // export

      File tmp = File.createTempFile("__exojcr_TestSysView__", ".tmp");

      OutputStream xmlOut = PrivilegedFileHelper.fileOutputStream(tmp);
      sysview.getSession().exportSystemView(ref.getPath(), xmlOut, false, false);
      xmlOut.close();

      xmlContent = tmp;
   }

   public void testExportUuid_IMPORT_UUID_COLLISION_REMOVE_EXISTING() throws Exception
   {
      Node source = sysview.getNode(SOURCE_NAME);
      String uuid = source.getProperty("jcr:uuid").getString();

      Node importTarget = sysview.addNode("import target");
      sysview.save();

      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      sysview.save();

      // check uuid

      assertFalse("A node must  not exists " + source.getPath(), sysview.hasNode(SOURCE_NAME));

      String importedUuid = importTarget.getNode(SOURCE_NAME).getProperty("jcr:uuid").getString();
      assertTrue("Uuids must be same. " + uuid + " = " + importedUuid, uuid.equals(importedUuid));

      // try one more (for same-name sibling nodes test), mus replace before
      // imported node
      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      sysview.save();

      // check sns...

      assertFalse("Same-name sibling node must is not exists. ", importTarget.hasNode(SOURCE_NAME + "[2]"));
      assertTrue(importTarget.hasNode(SOURCE_NAME));
      String importedSNSUuid = importTarget.getNode(SOURCE_NAME).getProperty("jcr:uuid").getString();
      assertTrue("Uuids must be same. " + uuid + " = " + importedSNSUuid, uuid.equals(importedSNSUuid));
   }

   public void testExportUuid_IMPORT_UUID_COLLISION_REPLACE_EXISTING() throws Exception
   {
      Node source = sysview.getNode(SOURCE_NAME);

      source.getNode("jcr:content").setProperty("New property 1, boolean", false);
      source.getNode("jcr:content").setProperty("New property 2, string", "STRING 1");

      String uuid = source.getProperty("jcr:uuid").getString();

      Node importTarget = sysview.addNode("import target");
      sysview.save();

      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

      sysview.save();

      // check...

      Node target = sysview.getNode(SOURCE_NAME);

      String importedUuid = target.getProperty("jcr:uuid").getString();
      assertTrue("Uuids must be same. " + uuid + " = " + importedUuid, uuid.equals(importedUuid));

      assertFalse("A imported node must has no property 'New property 1, boolean' " + target.getPath(), target
         .hasProperty("jcr:content/New property 1, boolean"));
      assertFalse("A imported node must has no property 'New property 2, string' " + target.getPath(), target
         .hasProperty("jcr:content/New property 2, string"));

      // create one more same-name sibling node
      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

      sysview.save();

      // check sns...

      target = sysview.getNode(SOURCE_NAME);
      String importedSNSUuid = target.getProperty("jcr:uuid").getString();
      assertTrue("Uuids must be same. " + uuid + " = " + importedSNSUuid, uuid.equals(importedSNSUuid));

      assertTrue("Uuid of SNS replaced node must be different. " + importedSNSUuid + " != " + importedSNSUuid,
         importedSNSUuid.equals(importedSNSUuid));

      assertFalse("A imported node must has no property 'New property 1, boolean' " + target.getPath(), target
         .hasProperty("jcr:content/New property 1, boolean"));
      assertFalse("A imported node must has no property 'New property 2, string' " + target.getPath(), target
         .hasProperty("jcr:content/New property 2, string"));
   }

   public void testExportUuid_IMPORT_UUID_COLLISION_THROW() throws Exception
   {
      Node source = sysview.getNode(SOURCE_NAME);
      source.getProperty("jcr:uuid").getString();

      Node importTarget = sysview.addNode("import target");
      sysview.save();

      try
      {
         sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
            ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

         fail("An exception ItemExistsException must be throwed. Node with same uuid already exists");
      }
      catch (ItemExistsException e)
      {
         // ok
      }

      // one more time...:)
      try
      {
         sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
            ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
         fail("An exception ItemExistsException must be throwed. Node with same uuid already exists. TEST CYCLE2");
      }
      catch (ItemExistsException e)
      {
         // ok
      }
   }

   public void testExportUuid_IMPORT_UUID_CREATE_NEW() throws Exception
   {
      String uuid = sysview.getNode(SOURCE_NAME).getProperty("jcr:uuid").getString();

      Node importTarget = sysview.addNode("import target");
      sysview.save();

      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

      sysview.save();

      // check uuid

      String importedUuid = importTarget.getNode(SOURCE_NAME).getProperty("jcr:uuid").getString();
      assertFalse("Uuids must be different. " + uuid + " != " + importedUuid, uuid.equals(importedUuid));

      // create one more same-name sibling node
      sysview.getSession().importXML(importTarget.getPath(), PrivilegedFileHelper.fileInputStream(xmlContent),
         ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

      sysview.save();

      // check sns...
      String importedSNSUuid = importTarget.getNode(SOURCE_NAME + "[2]").getProperty("jcr:uuid").getString();
      assertFalse("Uuids must be different. " + uuid + " != " + importedSNSUuid, uuid.equals(importedSNSUuid));
      assertFalse("Uuids must be different. " + importedSNSUuid + " != " + importedUuid, importedSNSUuid
         .equals(importedUuid));

      // ...temp check
      InputStream anyBinary =
         importTarget.getNode(SOURCE_NAME + "[2]/jcr:content").getProperty("anyBinary").getStream();
      assertEquals("Stream length must be same", BIN_STRING.length(), anyBinary.available());
      assertEquals("Stream content must be same", BIN_STRING, importTarget.getNode(SOURCE_NAME + "[2]/jcr:content")
         .getProperty("anyBinary").getString());
   }

   public void testImportSystemViewContentHandlerInvalidChildNodeType() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "nt:folder");

      Node exportRoot = root.addNode("exportRoot", "exo:article");

      exportRoot.setProperty("exo:title", "title");
      exportRoot.setProperty("exo:text", "text");

      session.save();

      byte[] content = serialize(exportRoot, true, true);

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

   public void testImportSystemViewStreamInvalidChildNodeType() throws Exception
   {

      Node testRoot = root.addNode("testRoot", "nt:folder");

      Node exportRoot = root.addNode("exportRoot", "exo:article");

      exportRoot.setProperty("exo:title", "title");
      exportRoot.setProperty("exo:text", "text");

      session.save();
      // try {
      // testRoot.addNode("test", "exo:article");
      // fail();
      // } catch (RepositoryException e) {
      // }

      byte[] content = serialize(exportRoot, true, true);

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

   public void testImportSysView() throws RepositoryException, InvalidSerializedDataException,
      ConstraintViolationException, IOException, ItemExistsException
   {

      root.addNode("test");
      session.importXML("/test", new ByteArrayInputStream(SYSTEM_VIEW_CONTENT.getBytes()), 0);
      session.save();

      Node testRoot = session.getRootNode().getNode("test");
      NodeIterator iterator = testRoot.getNodes();
      assertEquals(1, iterator.getSize());

      // log.debug(">>"+session.getWorkspaceDataContainer());

      iterator = testRoot.getNode("exo:test/childNode").getNodes();
      assertEquals(2, iterator.getSize());

      Property property = testRoot.getProperty("exo:test/childNode/childNode3/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode/childNode2/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode4/jcr:test");
      assertEquals(2, property.getValues().length);
      assertEquals("val1", property.getValues()[0].getString());
   }

   public void testImportSysViewFormatted() throws RepositoryException, InvalidSerializedDataException,
      ConstraintViolationException, IOException, ItemExistsException
   {

      root.addNode("testFormatted");
      session.importXML("/testFormatted", new ByteArrayInputStream(SYSTEM_VIEW_CONTENT_FORMATTED.getBytes()), 0);
      session.save();

      Node testRoot = session.getRootNode().getNode("testFormatted");
      NodeIterator iterator = testRoot.getNodes();
      assertEquals(1, iterator.getSize());

      // log.debug(">>"+session.getWorkspaceDataContainer());

      iterator = testRoot.getNode("exo:test/childNode").getNodes();
      assertEquals(2, iterator.getSize());

      Property property = testRoot.getProperty("exo:test/childNode/childNode3/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode/childNode2/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode4/jcr:test");
      assertEquals(2, property.getValues().length);
      assertEquals("val1", property.getValues()[0].getString());
   }

   public void testImportSysViewContentHandler() throws Exception
   {

      // session.getRootNode().addNode("test", "nt:unstructured");

      XMLReader reader = XMLReaderFactory.createXMLReader();
      root.addNode("test");
      reader.setContentHandler(session.getImportContentHandler("/test", 0));
      InputSource inputSource = new InputSource(new ByteArrayInputStream(SYSTEM_VIEW_CONTENT.getBytes()));
      reader.parse(inputSource);

      session.save();

      Node testRoot = session.getRootNode().getNode("test");
      NodeIterator iterator = testRoot.getNodes();
      assertEquals(1, iterator.getSize());

      // log.debug(">>"+session.getWorkspaceDataContainer());

      iterator = testRoot.getNode("exo:test/childNode").getNodes();
      assertEquals(2, iterator.getSize());

      Property property = testRoot.getProperty("exo:test/childNode/childNode3/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode/childNode2/jcr:content/jcr:data");
      assertEquals("this is the binary content", property.getString());

      property = testRoot.getProperty("exo:test/childNode4/jcr:test");
      assertEquals(2, property.getValues().length);
      assertEquals("val1", property.getValues()[0].getString());
   }

   public void testSysImportUnExistingPropertyDefinition() throws Exception
   {
      InvocationContext context = new InvocationContext();
      context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);
      try
      {
         ((ExtendedSession)session).importXML(root.getPath(),
            new ByteArrayInputStream(SYSTEM_VIEW_CONTENT2.getBytes()), 0, context);
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
      context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, false);
      try
      {
         ((ExtendedSession)session).importXML(root.getPath(),
            new ByteArrayInputStream(SYSTEM_VIEW_CONTENT2.getBytes()), 0, context);
         session.save();

      }
      catch (RepositoryException e)
      {
         // e.printStackTrace();
         fail();
      }

   }

   public void testImportReferenceable() throws Exception
   {
      Node testRoot = root.addNode("testImportReferenceable");
      root.save();
      testRoot.addMixin("mix:referenceable");
      root.save();
      String uuid = testRoot.getUUID();

      byte[] buf = serialize(testRoot, true, true);

      testRoot.remove();
      root.save();

      deserialize(root, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
         new ByteArrayInputStream(buf));

      root.save();

      testRoot = root.getNode("testImportReferenceable");
      assertTrue(testRoot.isNodeType("mix:referenceable"));
      assertEquals(uuid, testRoot.getUUID());

   }

   @Override
   protected void tearDown() throws Exception
   {

      if (xmlContent != null)
      {
         xmlContent.delete();
      }

      super.tearDown();
   }
}
