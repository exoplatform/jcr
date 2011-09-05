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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 02.03.2007 14:31:17
 * 
 * @version $Id: TestReplicationVersionRestore.java 02.03.2007 14:31:17 rainfox
 */

public class ReplicationVersionRestoreTest extends BaseReplicationTest
{

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.ReplicationVersionRestoreTest");

   public void testRestore() throws Exception
   {
      Node srcVersionNode = root.addNode("Version node 1");
      srcVersionNode.setProperty("jcr:data", "Base version");
      srcVersionNode.addMixin("mix:versionable");
      session.save();

      Thread.sleep(5 * 1000);

      Node destVersionNode = root2.getNode("Version node 1");
      assertEquals("Base version", destVersionNode.getProperty("jcr:data").getString());

      srcVersionNode.checkin();
      session.save();

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 1");
      session.save();

      Thread.sleep(5 * 1000);

      assertEquals("version 1", destVersionNode.getProperty("jcr:data").getString());

      srcVersionNode.checkin();
      session.save();

      srcVersionNode.checkout();
      srcVersionNode.setProperty("jcr:data", "version 2");
      session.save();

      Thread.sleep(5 * 1000);

      assertEquals("version 2", destVersionNode.getProperty("jcr:data").getString());

      Version baseVersion = srcVersionNode.getBaseVersion();
      srcVersionNode.restore(baseVersion, true);
      session.save();

      Thread.sleep(5 * 1000);

      assertEquals("version 1", destVersionNode.getProperty("jcr:data").getString());

      Version baseVersion1 = srcVersionNode.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersionNode.restore(restoreToBaseVersion, true);
      session.save();

      Thread.sleep(5 * 1000);

      assertEquals("Base version", destVersionNode.getProperty("jcr:data").getString());
   }

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

      log.info("FILE for VERSION #1 : file size = " + tempFile.length() + " bytes");
      log.info("FILE for VERSION #2 : file size = " + tempFile2.length() + " bytes");
      log.info("FILE for VERSION #3 : file size = " + tempFile3.length() + " bytes");

      Node srcVersionNode = root.addNode("nt_file_node", "nt:file");
      Node contentNode = srcVersionNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
      srcVersionNode.addMixin("mix:versionable");

      session.save();

      log.info("SAVED");

      Node srcVersion = root.getNode("nt_file_node");
      log.info("GET");

      long time = 10 * 1000;

      Thread.sleep(time);

      Node destVersionNode = root2.getNode("nt_file_node").getNode("jcr:content");
      log.info("ADD VERSION #1 : file size = " + destVersionNode.getProperty("jcr:data").getStream().available()
         + " bytes");
      compareStream(new FileInputStream(tempFile), destVersionNode.getProperty("jcr:data").getStream());

      srcVersion.checkin();
      session.save();

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile2));
      session.save();

      Thread.sleep(time);

      log.info("ADD VERSION #2 : file size = " + destVersionNode.getProperty("jcr:data").getStream().available()
         + " bytes");
      compareStream(new FileInputStream(tempFile2), destVersionNode.getProperty("jcr:data").getStream());

      srcVersion.checkin();
      session.save();

      Thread.sleep(time);

      srcVersion.checkout();
      srcVersionNode.getNode("jcr:content").setProperty("jcr:data", new FileInputStream(tempFile3));
      session.save();

      Thread.sleep(time);

      log.info("ADD VERSION #3 : file size = " + destVersionNode.getProperty("jcr:data").getStream().available()
         + " bytes");
      compareStream(new FileInputStream(tempFile3), destVersionNode.getProperty("jcr:data").getStream());

      Version baseVersion = srcVersion.getBaseVersion();
      srcVersion.restore(baseVersion, true);
      session.save();

      Thread.sleep(time);

      compareStream(new FileInputStream(tempFile2), destVersionNode.getProperty("jcr:data").getStream());

      Version baseVersion1 = srcVersion.getBaseVersion();
      Version[] predesessors = baseVersion1.getPredecessors();
      Version restoreToBaseVersion = predesessors[0];

      srcVersion.restore(restoreToBaseVersion, true); // HERE
      session.save();

      Thread.sleep(time);

      compareStream(new FileInputStream(tempFile), destVersionNode.getProperty("jcr:data").getStream());

      // restore from destination node
      Node destVersion = destVersionNode.getParent();

      Version baseVersion2 = destVersion.getBaseVersion();
      Version[] predesessors2 = baseVersion2.getSuccessors();
      Version restoreToBaseVersion_2 = predesessors2[0];

      destVersion.restore(restoreToBaseVersion_2, true);
      session.save();

      Thread.sleep(time);

      compareStream(new FileInputStream(tempFile2), srcVersionNode.getNode("jcr:content").getProperty("jcr:data")
         .getStream());
   }
}
