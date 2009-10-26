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

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 28.02.2007 10:59:36
 * 
 * @version $Id: TestReplicationCopyNode.java 28.02.2007 10:59:36 rainfox
 */

public class ReplicationCopyMoveNodeTest extends BaseReplicationTest
{

   private static final Log log = ExoLogger.getLogger(ReplicationCopyMoveNodeTest.class);

   public void testSessionMove() throws Exception
   {

      Node file = root.addNode("testSessionMove", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", "this is the content");
      contentNode.setProperty("jcr:mimeType", "text/html");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      session.move("/testSessionMove", "/testSessionMove1");

      session.save();

      assertNotNull(session.getItem("/testSessionMove1"));
      assertNotNull(session.getItem("/testSessionMove1/childNode2/jcr:content"));

      Thread.sleep(7 * 1000);

      // COMPARE REPLICATION DATA
      assertNotNull(session2.getItem("/testSessionMove1"));
      assertNotNull(session2.getItem("/testSessionMove1/childNode2/jcr:content"));

      Node srcNode = root.getNode("testSessionMove1").getNode("childNode2").getNode("jcr:content");
      Node destNode = root2.getNode("testSessionMove1").getNode("childNode2").getNode("jcr:content");

      assertEquals(srcNode.getProperty("jcr:data").getString(), destNode.getProperty("jcr:data").getString());
      assertEquals(srcNode.getProperty("jcr:mimeType").getString(), destNode.getProperty("jcr:mimeType").getString());
      assertEquals(srcNode.getProperty("jcr:lastModified").getString(), destNode.getProperty("jcr:lastModified")
         .getString());
   }

   public void testCopy() throws Exception
   {

      Node file = root.addNode("testCopy", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", "this is the content");
      contentNode.setProperty("jcr:mimeType", "text/html");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      workspace.copy("/testCopy", "/testCopy1");

      Thread.sleep(7 * 1000);

      // COMPARE REPLICATION DATA
      assertNotNull(session2.getItem("/testCopy1"));
      assertNotNull(session2.getItem("/testCopy1/childNode2"));
      assertNotNull(session2.getItem("/testCopy1/childNode2/jcr:content"));
      assertNotNull(session2.getItem("/testCopy"));

      Node srcNode = root.getNode("testCopy1").getNode("childNode2").getNode("jcr:content");
      Node destNode = root2.getNode("testCopy1").getNode("childNode2").getNode("jcr:content");

      assertEquals(srcNode.getProperty("jcr:data").getString(), destNode.getProperty("jcr:data").getString());
      assertEquals(srcNode.getProperty("jcr:mimeType").getString(), destNode.getProperty("jcr:mimeType").getString());
      assertEquals(srcNode.getProperty("jcr:lastModified").getString(), destNode.getProperty("jcr:lastModified")
         .getString());
   }

   public void testMove() throws Exception
   {

      Node file = root.addNode("testMove", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", "this is the content");
      contentNode.setProperty("jcr:mimeType", "text/html");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      workspace.move("/testMove", "/testMove1");

      Thread.sleep(7 * 1000);

      // COMPARE REPLICATION DATA
      assertNotNull(session2.getItem("/testMove1"));
      assertNotNull(session2.getItem("/testMove1/childNode2"));
      assertNotNull(session2.getItem("/testMove1/childNode2/jcr:content"));

      Node srcNode = root.getNode("testMove1").getNode("childNode2").getNode("jcr:content");
      Node destNode = root2.getNode("testMove1").getNode("childNode2").getNode("jcr:content");

      assertEquals(srcNode.getProperty("jcr:data").getString(), destNode.getProperty("jcr:data").getString());
      assertEquals(srcNode.getProperty("jcr:mimeType").getString(), destNode.getProperty("jcr:mimeType").getString());
      assertEquals(srcNode.getProperty("jcr:lastModified").getString(), destNode.getProperty("jcr:lastModified")
         .getString());
   }

   public void testBigDataMove() throws Exception
   {

      File tempFile = File.createTempFile("tempFile", "doc");
      tempFile.deleteOnExit();

      FileOutputStream fos = new FileOutputStream(tempFile);

      String content = "this is the content";

      for (int i = 0; i < 15000; i++)
         fos.write((i + " " + content).getBytes());

      fos.close();

      log.info("MOVE: file size = " + tempFile.length() + " bytes");

      Node file = root.addNode("testMove_", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "text/plain");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();
      log.info("FIRST");

      workspace.move("/testMove_", "/testMove_dest");

      session.save();
      log.info("MOVED");

      log.info("Sleep 15 seconds");
      Thread.yield();
      Thread.sleep(15 * 1000);

      // COMPARE REPLICATION DATA
      assertNotNull(session2.getItem("/testMove_dest"));
      assertNotNull(session2.getItem("/testMove_dest/childNode2"));
      assertNotNull(session2.getItem("/testMove_dest/childNode2/jcr:content"));

      Node srcNode = root.getNode("testMove_dest").getNode("childNode2").getNode("jcr:content");
      Node destNode = root2.getNode("testMove_dest").getNode("childNode2").getNode("jcr:content");

      log.info("source data size      = " + srcNode.getProperty("jcr:data").getStream().available());
      log.info("destination data size = " + destNode.getProperty("jcr:data").getStream().available());

      assertEquals(srcNode.getProperty("jcr:data").getStream().available(), destNode.getProperty("jcr:data")
         .getStream().available());
      assertEquals(srcNode.getProperty("jcr:mimeType").getString(), destNode.getProperty("jcr:mimeType").getString());
      assertEquals(srcNode.getProperty("jcr:lastModified").getString(), destNode.getProperty("jcr:lastModified")
         .getString());
   }

}
