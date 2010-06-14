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
package org.exoplatform.services.jcr.load.blob;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. NOTE: Make sure you have the files pointed below!
 */

public class TestMoveBinaryValue1 extends JcrAPIBaseTest
{

   private Node testBinaryValue = null;

   private static int FILES_COUNT = 1000;

   // -------------- TEST FILE ------------------
   private static String TEST_FILE = null; // URL_SMALL_FILE

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testBinaryValue = root.addNode("testBinaryValue");
      session.save();
   }

   public void _testLocalBigFiles() throws Exception
   {
      Node testLocalBigFiles = testBinaryValue.addNode("testLocalBigFiles");
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start

      // 300 Kb
      TEST_FILE = PrivilegedFileHelper.getAbsolutePath(createBLOBTempFile(300));

      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localBigFile = testLocalBigFiles.addNode("bigFile" + i, "nt:file");
         Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
         // contentNode.setProperty("jcr:encoding", "UTF-8");
         InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE);
         contentNode.setProperty("jcr:data", is);
         contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
         is.close();
         log.info("Data is set: " + TEST_FILE);
         // contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }
      log.info("Saving: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      session.save();
      log.info("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      try
      {
         Node dstNode = testLocalBigFiles.addNode("dst");
         for (int i = 0; i < FILES_COUNT; i++)
         {
            session.move(testLocalBigFiles.getPath() + "/" + "bigFile" + i, dstNode.getPath() + "/" + "bigFile" + i);
         }
         session.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
   }

   public void testWsLocalBigFiles() throws Exception
   {
      Node testLocalBigFiles = testBinaryValue.addNode("testLocalBigFiles");
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start

      // 45 Kb
      TEST_FILE = PrivilegedFileHelper.getAbsolutePath(createBLOBTempFile(45));

      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localBigFile = testLocalBigFiles.addNode("bigFile" + i, "nt:file");
         Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
         // contentNode.setProperty("jcr:encoding", "UTF-8");
         InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE);
         contentNode.setProperty("jcr:data", is);
         contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
         is.close();
         log.info("Data is set: " + TEST_FILE);
         // contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }
      log.info("Saving: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      session.save();
      log.info("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      try
      {
         Node dstNode = testLocalBigFiles.addNode("dst");
         session.save();
         for (int i = 0; i < FILES_COUNT; i++)
         {
            long time = System.currentTimeMillis();
            workspace.move(testLocalBigFiles.getPath() + "/" + "bigFile" + i, dstNode.getPath() + "/" + "bigFile" + i);
            log.info("Move" + ((System.currentTimeMillis() - time)) + "s");
         }

      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      testBinaryValue.remove();
      root.save();
      super.tearDown();
   }
}
