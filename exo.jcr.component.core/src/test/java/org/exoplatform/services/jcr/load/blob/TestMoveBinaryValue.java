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

package org.exoplatform.services.jcr.load.blob;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * NOTE: Make sure you have the files pointed below!
 */

public class TestMoveBinaryValue extends JcrAPIBaseTest
{

   private Node testBinaryValue = null;

   private static int FILES_COUNT = 100;

   // -------------- TEST FILE ------------------
   private static String TEST_FILE = null; // URL_SMALL_FILE

   public void setUp() throws Exception
   {
      super.setUp();
      testBinaryValue = root.addNode("testBinaryValue");
      session.save();
   }

   public void testLocalBigFiles() throws Exception
   {
      Node testLocalBigFiles = testBinaryValue.addNode("testLocalBigFiles");
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start

      // 300 Kb
      TEST_FILE = createBLOBTempFile(300).getAbsolutePath();

      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localBigFile = testLocalBigFiles.addNode("bigFile" + i, "nt:file");
         Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
         // contentNode.setProperty("jcr:encoding", "UTF-8");
         InputStream is = new FileInputStream(TEST_FILE);
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

   protected void tearDown() throws Exception
   {
      testBinaryValue.remove();
      root.save();
      super.tearDown();
   }
}
