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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Date: 08.05.2008 <br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestRestoreWorkspaceInitializer.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */
public class TestRestoreWorkspaceInitializer extends JcrImplBaseTest
{

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
}
