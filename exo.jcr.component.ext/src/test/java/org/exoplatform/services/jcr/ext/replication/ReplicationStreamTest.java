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
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 16.01.2007 15:21:45
 * 
 * @version $Id: TestReplicationStream.java 16.01.2007 15:21:45 rainf0x
 */

public class ReplicationStreamTest extends BaseReplicationTest
{

   private static final Log log = ExoLogger.getLogger(ReplicationStreamTest.class);

   public void testAddNode() throws Exception
   {
      long start, end;
      byte[] buf = new byte[1024];
      int fileSize = 50000; // KB

      File tempFile = File.createTempFile("tempF", "_");
      FileOutputStream fos = new FileOutputStream(tempFile);

      for (int i = 0; i < buf.length; i++)
         buf[i] = (byte)(i % 255);

      for (int i = 0; i < fileSize; i++)
         fos.write(buf);
      fos.close();

      Node test = root.addNode("cms2").addNode("test");
      start = System.currentTimeMillis(); // to get the time of start

      Node cool = test.addNode("nnn", "nt:file");
      Node contentNode = cool.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
      contentNode.setProperty("jcr:mimeType", "application/octet-stream");
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      try
      {
         session.save();
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Error Save!!!");
      }

      end = System.currentTimeMillis();

      System.out.println("The time of the adding of nt:file : " + ((end - start) / 1000) + " sec");

      // COMPARE REPLICATION DATA
      Node sourceNode = root.getNode("cms2").getNode("test").getNode("nnn").getNode("jcr:content");
      InputStream fis = sourceNode.getProperty("jcr:data").getStream();

      Thread.sleep(25 * 1000);

      Node desinationNode = root2.getNode("cms2").getNode("test").getNode("nnn").getNode("jcr:content");
      InputStream fis2 = desinationNode.getProperty("jcr:data").getStream();

      compareStream(fis, fis2);

      assertEquals(sourceNode.getProperty("jcr:encoding").getString(), desinationNode.getProperty("jcr:encoding")
         .getString());

      assertEquals(sourceNode.getProperty("jcr:lastModified").getString(), desinationNode.getProperty(
         "jcr:lastModified").getString());

      // delete

      Node srcParent = sourceNode.getParent();
      srcParent.remove();
      session.save();

      Thread.sleep(5 * 1000);

      try
      {
         Node destinationRemovablesNode = (Node)session2.getItem("/cms2/test/nnn");
         fail("The node " + destinationRemovablesNode.getPath() + "must be removed");
      }
      catch (PathNotFoundException pe)
      {
         // ok
      }
   }

   public void tearDown() throws Exception
   {
      Thread.sleep(10 * 1000);
      log.info("Sleep 10 sec");
      super.tearDown();
   }

}
