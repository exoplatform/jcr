/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases.version;

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
 * <br>
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

      FileOutputStream fos = new FileOutputStream(tempFile);
      FileOutputStream fos2 = new FileOutputStream(tempFile2);
      FileOutputStream fos3 = new FileOutputStream(tempFile3);

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

      Node file = root.addNode("nt_file_node", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      file.addMixin("mix:versionable");
      session.save();


      file.checkin(); // v1
      file.checkout(); // file.getNode("jcr:content").getProperty("jcr:data").getStream()
      file.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile2));
      session.save();

      compareStream(new FileInputStream(tempFile2), contentNode.getProperty("jcr:data").getStream());

      file.checkin(); // v2
      file.checkout();
      file.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile3));
      session.save();

      compareStream(new FileInputStream(tempFile3), contentNode.getProperty("jcr:data").getStream());

      // restore version v2
      Version v2 = file.getBaseVersion();
      file.restore(v2, true);

      compareStream(new FileInputStream(tempFile2), contentNode.getProperty("jcr:data").getStream());

      // restore version v1
      Version v1 = file.getBaseVersion().getPredecessors()[0];
      file.restore(v1, true); // HERE

      compareStream(new FileInputStream(tempFile), contentNode.getProperty("jcr:data").getStream());

      // restore version v2 again
      file.restore(v2, true);

      compareStream(new FileInputStream(tempFile2), file.getNode("jcr:content").getProperty("jcr:data").getStream());
   }
}
