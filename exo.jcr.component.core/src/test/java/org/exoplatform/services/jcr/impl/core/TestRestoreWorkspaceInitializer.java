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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.xml.stream.XMLStreamException;

/**
 * Created by The eXo Platform SAS. <br>
 * 
 * Date: 08.05.2008 <br>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestRestoreWorkspaceInitializer.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */
public class TestRestoreWorkspaceInitializer extends JcrImplBaseTest
{
   private static final String RESTORE_MARKER = "MARKER-EXO-92938";

   /**
    * Make export of system ws with custom NT with multivalued properties.
    * 
    * @throws Exception
    */
   public void _testExportSystemWorkspace() throws Exception
   {

      Node multiv = root.addNode("multivaluedProperty", "exojcrtest:multiValued");
      multiv.setProperty("exojcrtest:multiValuedString", new String[]{"value1"});

      Value v1 = session.getValueFactory().createValue(Calendar.getInstance());
      multiv.setProperty("exojcrtest:multiValuedDate", new Value[]{v1});

      JCRName jcrName = session.getLocationFactory().parseJCRName("exojcrtest:dummyName");
      v1 = session.getValueFactory().createValue(jcrName);
      multiv.setProperty("exojcrtest:multiValuedName", new Value[]{v1});

      Node blob = root.addNode("binaryTest");
      File f;
      InputStream is;
      blob.setProperty("blob", is = new FileInputStream(f = createBLOBTempFile(2 * 1024))); // 2M

      root.save();

      is.close();
      f.renameTo(new File("./sv_export_binary.bin"));

      File outf = new File("./sv_export_root.xml");
      FileOutputStream out = new FileOutputStream(outf);
      session.exportWorkspaceSystemView(out, false, false);
      out.close();
   }

   /**
    * Should be used with RestoreWorkspaceInitializer and export file obtained in the test
    * testExportSystemWorkspace().
    * 
    * Sample config: <initializer
    * class="org.exoplatform.services.jcr.impl.core.RestoreWorkspaceInitializer"> <properties>
    * <property name="restore-path" value="./sv_export_root.xml"/> </properties> </initializer>
    * 
    * @throws Exception
    */
   public void testCheckRestoreSystemWorkspace() throws Exception
   {

      Session ws1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws1");
      Node ws1root = ws1.getRootNode();
      if (ws1root.hasProperty("1_common/cargo/cargo/0.5/cargo-0.5.jar/jcr:content/jcr:data"))
      {
         InputStream is =
            ws1root.getProperty("1_common/cargo/cargo/0.5/cargo-0.5.jar/jcr:content/jcr:data").getStream();
         FileOutputStream fout = new FileOutputStream("./cargo-0.5.jar");
         int r = -1;
         byte[] b = new byte[1024];
         while ((r = is.read(b)) >= 0)
         {
            fout.write(b, 0, r);
         }
         fout.close();
      }

      if (root.hasNode("multivaluedProperty"))
      {
         Node multiv = root.getNode("multivaluedProperty");
         try
         {
            Property p = multiv.getProperty("exojcrtest:multiValuedString");
            p.getValues();
            p = multiv.getProperty("exojcrtest:multiValuedDate");
            p.getValues();
            p = multiv.getProperty("exojcrtest:multiValuedName");
            p.getValues();

            compareStream(new FileInputStream("./sv_export_binary.bin"), root.getNode("binaryTest").getProperty("blob")
               .getStream());
         }
         catch (ValueFormatException e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
      } // else skip test
   }

   public void testRestoreRefusesExternalDtd() throws Exception
   {
      File dtd = createRestoreFile(".dtd", "<!ENTITY m \"" + RESTORE_MARKER + "\">");
      assertRestoreRefused("<?xml version='1.0'?><!DOCTYPE sv:node SYSTEM '" + dtd.toURI() + "'>" + restoreRoot("&m;"));
   }

   public void testRestoreRefusesExternalEntity() throws Exception
   {
      File entity = createRestoreFile(".txt", RESTORE_MARKER);
      assertRestoreRefused("<?xml version='1.0'?><!DOCTYPE sv:node [<!ENTITY e SYSTEM '" + entity.toURI() + "'>]>"
         + restoreRoot("&e;"));
   }

   public void testRestoreReadsInternalDtdSubset() throws Exception
   {
      PlainChangesLog changes =
         read("<?xml version='1.0'?><!DOCTYPE sv:node [<!ENTITY i 'internal value'>]>" + restoreRoot("&i;"));
      assertEquals("internal value", restoredMarker(changes, null));
   }

   public void testRestoreReadsWorkspaceExport() throws Exception
   {
      Node node = root.addNode("restoreExport");
      node.setProperty("marker", "exported value");
      session.save();
      File export = File.createTempFile("restore", ".xml");
      export.deleteOnExit();
      try (FileOutputStream out = new FileOutputStream(export))
      {
         session.exportWorkspaceSystemView(out, false, false);
      }

      PlainChangesLog changes = initializer(export).read();

      assertEquals("exported value", restoredMarker(changes, "restoreExport"));
   }

   private void assertRestoreRefused(String xml) throws Exception
   {
      try
      {
         PlainChangesLog changes = read(xml);
         fail("Restore file read, marker: " + restoredMarker(changes, null));
      }
      catch (XMLStreamException e)
      {
         assertTrue(e.getMessage(), e.getMessage().contains("External DTD or entity not allowed"));
         assertFalse(e.getMessage().contains(RESTORE_MARKER));
      }
   }

   private PlainChangesLog read(String xml) throws Exception
   {
      return initializer(createRestoreFile(".xml", xml)).read();
   }

   private SysViewWorkspaceInitializer initializer(File restoreFile) throws Exception
   {
      return new SysViewWorkspaceInitializer(repository.getConfiguration().getWorkspaceEntries().get(0),
         repository.getConfiguration(), null, (NamespaceRegistryImpl)repository.getNamespaceRegistry(),
         session.getLocationFactory(), null, session.getValueFactory(), null, restoreFile.getAbsolutePath());
   }

   private File createRestoreFile(String suffix, String content) throws Exception
   {
      File file = File.createTempFile("restore", suffix);
      file.deleteOnExit();
      Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
      return file;
   }

   private String restoreRoot(String marker)
   {
      return "<sv:node xmlns:sv='http://www.jcp.org/jcr/sv/1.0' xmlns:jcr='http://www.jcp.org/jcr/1.0' "
         + "xmlns:exo='http://www.exoplatform.com/jcr/exo/1.0' sv:name='jcr:root' exo:id='00exo0jcr0root0uuid0000000000000'>"
         + "<sv:property sv:name='jcr:primaryType' sv:type='Name' exo:id='restoreprimarytype'>"
         + "<sv:value>nt:unstructured</sv:value></sv:property>"
         + "<sv:property sv:name='marker' sv:type='String' exo:id='restoremarker'><sv:value>" + marker
         + "</sv:value></sv:property></sv:node>";
   }

   private String restoredMarker(PlainChangesLog changes, String parentName) throws Exception
   {
      for (ItemState state : changes.getAllStates())
      {
         QPath path = state.getData().getQPath();
         String parent = path.getDepth() > 1 ? path.makeParentPath().getName().getName() : null;
         if (!state.isNode() && "marker".equals(path.getName().getName())
            && (parentName == null ? parent == null : parentName.equals(parent)))
         {
            return new String(((PropertyData)state.getData()).getValues().get(0).getAsByteArray(),
               StandardCharsets.UTF_8);
         }
      }
      return null;
   }
}
