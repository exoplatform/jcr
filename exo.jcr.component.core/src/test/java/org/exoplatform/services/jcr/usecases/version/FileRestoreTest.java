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
package org.exoplatform.services.jcr.usecases.version;

import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 02.06.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: FileRestoreTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class FileRestoreTest extends BaseUsecasesTest
{

   public void testBigFileRestore() throws Exception
   {

      File tempFile = File.createTempFile("tempFile", "doc");
      File tempFile2 = File.createTempFile("tempFile", "doc");
      File tempFile3 = File.createTempFile("tempFile", "doc");
      tempFile.deleteOnExit();
      tempFile2.deleteOnExit();
      tempFile3.deleteOnExit();

      FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);
      FileOutputStream fos2 = PrivilegedFileHelper.fileOutputStream(tempFile2);
      FileOutputStream fos3 = PrivilegedFileHelper.fileOutputStream(tempFile3);

      String content = "this is the content #1";
      String content2 = "this is the content #2_";
      String content3 = "this is the content #3__";

      for (int i = 0; i < 15000; i++)
      {
         fos.write((i + " " + content).getBytes());
         fos2.write((i + " " + content2).getBytes());
         fos3.write((i + " " + content3).getBytes());
      }

      fos.close();
      fos2.close();
      fos3.close();

      log.info("FILE for VERSION #1 : file size = " + tempFile.length() + " bytes");
      log.info("FILE for VERSION #2 : file size = " + tempFile2.length() + " bytes");
      log.info("FILE for VERSION #3 : file size = " + tempFile3.length() + " bytes");

      Node file = root.addNode("nt_file_node", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      file.addMixin("mix:versionable");
      session.save();

      log.info("SAVED");

      file.checkin(); // v1
      file.checkout(); // file.getNode("jcr:content").getProperty("jcr:data").getStream()
      file.getNode("jcr:content").setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile2));
      session.save();

      log
         .info("ADD VERSION #2 : file size = " + contentNode.getProperty("jcr:data").getStream().available() + " bytes");
      compareStream(PrivilegedFileHelper.fileInputStream(tempFile2), contentNode.getProperty("jcr:data").getStream());

      file.checkin(); // v2
      file.checkout();
      file.getNode("jcr:content").setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile3));
      session.save();

      log
         .info("ADD VERSION #3 : file size = " + contentNode.getProperty("jcr:data").getStream().available() + " bytes");
      compareStream(PrivilegedFileHelper.fileInputStream(tempFile3), contentNode.getProperty("jcr:data").getStream());

      // restore version v2
      Version v2 = file.getBaseVersion();
      file.restore(v2, true);

      compareStream(PrivilegedFileHelper.fileInputStream(tempFile2), contentNode.getProperty("jcr:data").getStream());

      // restore version v1
      Version v1 = file.getBaseVersion().getPredecessors()[0];
      file.restore(v1, true); // HERE

      compareStream(PrivilegedFileHelper.fileInputStream(tempFile), contentNode.getProperty("jcr:data").getStream());

      // restore version v2 again
      file.restore(v2, true);

      compareStream(PrivilegedFileHelper.fileInputStream(tempFile2), file.getNode("jcr:content").getProperty("jcr:data").getStream());
   }
}
