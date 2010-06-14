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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * NOTE: Make sure you have the files pointed below!
 */

public class TestBinaryValue extends JcrAPIBaseTest
{

   private Node testBinaryValue = null;

   private static int FILES_COUNT = 1;

   // private static String REMOTE_BIG_FILE = "\\\\Exooffice\\public\\Tmp\\resources\\BigFile.zip";

   // private static String REMOTE_SMALL_FILE =
   // "\\\\Exooffice\\public\\Tmp\\resources\\SmallFile.zip";

   // private static String URL_BIG_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/BigFile.zip";

   // private static String URL_SMALL_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/SmallFile.zip";

   // private static String URL_BIG_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/testBinaryValue/BigFile.zip";
   // private static String URL_BIG_FILE =
   // "http://exooffice:8080/jcr-webdav/repository/production/Lost.Season3.Preview.rus.avi";
   // private static String URL_BIG_FILE = "ftp://exoua.dnsalias.net/jcr/test/BigFile.zip";
   // private static String URL_BIG_MEDIA_FILE=
   // "ftp://exoua.dnsalias.net/pub/video/Lost.Season3.Preview.rus.avi";
   // private static String URL_VERYBIG_MEDIA_FILE =
   // "ftp://exoua.dnsalias.net/pub/video/9_11_xchcoin.avi";

   // private static String URL_SMALL_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/testBinaryValue/SmallFile.zip";
   // private static String URL_SMALL_FILE =
   // "http://exooffice:8080/jcr-webdav/repository/production/SmallFile.zip";
   // private static String URL_SMALL_FILE = "ftp://exoua.dnsalias.net/jcr/test/SmallFile.zip";

   // -------------- TEST FILE ------------------
   private static String TEST_FILE = null; // URL_SMALL_FILE

   @Override
   public void setUp() throws Exception
   {
      super.setUp();// .repository
      testBinaryValue = root.addNode("testBinaryValue");
      session.save();

      if (TEST_FILE == null)
      {
         // create test file
         TEST_FILE = createBLOBTempFile(2).getAbsolutePath();
      }
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
         InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE);
         contentNode.setProperty("jcr:data", is);
         contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
         is.close();
         System.out.println("Data is set: " + TEST_FILE);
         // contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }
      System.out.println("Saving: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      session.save();
      System.out.println("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");
   }

   /*
    * public void testLocalBigMediaFiles() throws Exception { Node testLocalBigMediaFiles =
    * testBinaryValue.addNode("testLocalBigMediaFiles"); long startTime, endTime; startTime =
    * System.currentTimeMillis(); // to get the time of start for (int i = 0; i < 1; i++) { Node
    * localBigMediaFile = testLocalBigMediaFiles.addNode("bigFile" + i, "nt:file"); Node contentNode
    * = localBigMediaFile.addNode("jcr:content", "nt:resource");
    * //contentNode.setProperty("jcr:encoding", "UTF-8"); contentNode.setProperty("jcr:data", new
    * FileInputStream(LOCAL_BIG_MEDIA_FILE)); contentNode.setProperty("jcr:mimeType",
    * "application/octet-stream "); //contentNode.setProperty("jcr:mimeType", "video/avi");
    * contentNode.setProperty("jcr:lastModified", Calendar.getInstance()); } session.save(); endTime
    * = System.currentTimeMillis(); log.info("Execution time after adding and saving (local big):" +
    * ((endTime - startTime) / 1000) + "s"); } // --------------- local stuff for anvanced testing
    * ------------- public void testLocalSmallFiles() throws Exception { Node testLocalSmallFiles =
    * testBinaryValue.addNode("testLocalSmallFiles"); long startTime, endTime; startTime =
    * System.currentTimeMillis(); // to get the time of start for (int i = 0; i < FILES_COUNT; i++) {
    * Node localSmallFile = testLocalSmallFiles.addNode("smallFile" + i, "nt:file"); Node contentNode
    * = localSmallFile.addNode("jcr:content", "nt:resource");
    * //contentNode.setProperty("jcr:encoding", "UTF-8"); contentNode.setProperty("jcr:data", new
    * FileInputStream(LOCAL_SMALL_FILE)); contentNode.setProperty("jcr:mimeType",
    * "application/octet-stream "); contentNode.setProperty("jcr:lastModified",
    * Calendar.getInstance()); } session.save(); endTime = System.currentTimeMillis();
    * log.info("Execution time after adding and saving: (local small)" + ((endTime - startTime) /
    * 1000) + "s"); // check streams //compareStream(PrivilegedFileHelper.fileInputStream(LOCAL_SMALL_FILE), //
    * testLocalSmallFiles.getProperty("smallFile0/jcr:content/jcr:data").getStream()); } public void
    * testRemoteBigFiles() throws Exception { Node testRemoteBigFiles =
    * testBinaryValue.addNode("testRemoteBigFiles"); long startTime, endTime; startTime =
    * System.currentTimeMillis(); // to get the time of start for (int i = 0; i < FILES_COUNT; i++) {
    * Node remoteBigFile = testRemoteBigFiles.addNode("bigFile" + i, "nt:file"); Node contentNode =
    * remoteBigFile.addNode("jcr:content", "nt:resource"); //contentNode.setProperty("jcr:encoding",
    * "UTF-8"); contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(REMOTE_BIG_FILE));
    * contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
    * contentNode.setProperty("jcr:lastModified", Calendar.getInstance()); } session.save(); endTime
    * = System.currentTimeMillis(); log.info("Execution time after adding and saving (remote big):" +
    * ((endTime - startTime) / 1000) + "s"); // check streams compareStream(new
    * FileInputStream(REMOTE_BIG_FILE),
    * testRemoteBigFiles.getProperty("bigFile0/jcr:content/jcr:data").getStream()); } public void
    * testRemoteSmallFiles() throws Exception { Node testRemoteSmallFiles =
    * testBinaryValue.addNode("testRemoteSmallFiles"); long startTime, endTime; startTime =
    * System.currentTimeMillis(); // to get the time of start for (int i = 0; i < FILES_COUNT; i++) {
    * Node remoteSmallFile = testRemoteSmallFiles.addNode("smallFile" + i, "nt:file"); Node
    * contentNode = remoteSmallFile.addNode("jcr:content", "nt:resource");
    * //contentNode.setProperty("jcr:encoding", "UTF-8"); contentNode.setProperty("jcr:data", new
    * FileInputStream(REMOTE_SMALL_FILE)); contentNode.setProperty("jcr:mimeType",
    * "application/octet-stream "); contentNode.setProperty("jcr:lastModified",
    * Calendar.getInstance()); } session.save(); endTime = System.currentTimeMillis();
    * log.info("Execution time after adding and saving: (remote small)" + ((endTime - startTime) /
    * 1000) + "s"); // check streams compareStream(PrivilegedFileHelper.fileInputStream(LOCAL_SMALL_FILE),
    * testRemoteSmallFiles.getProperty("smallFile0/jcr:content/jcr:data").getStream()); } public void
    * testUrlBigFiles() throws Exception { Node testUrlBigFiles =
    * testBinaryValue.addNode("testUrlBigFiles"); long startTime, endTime; startTime =
    * System.currentTimeMillis(); // to get the time of start for (int i = 0; i < FILES_COUNT; i++) {
    * Node urlBigFile = testUrlBigFiles.addNode("bigFile" + i, "nt:file"); Node contentNode =
    * urlBigFile.addNode("jcr:content", "nt:resource"); //contentNode.setProperty("jcr:encoding",
    * "UTF-8"); contentNode.setProperty("jcr:data", new URL(URL_BIG_FILE).openStream());
    * contentNode.setProperty("jcr:mimeType", "video/avi");
    * contentNode.setProperty("jcr:lastModified", Calendar.getInstance()); } session.save(); endTime
    * = System.currentTimeMillis(); log.info("Execution time after adding and saving: (url big)" +
    * ((endTime - startTime) / 1000) + "s"); } public void testUrlSmallFiles() throws Exception {
    * Node testUrlSmallFiles = testBinaryValue.addNode("testUrlSmallFiles"); long startTime, endTime;
    * startTime = System.currentTimeMillis(); // to get the time of start for (int i = 0; i <
    * FILES_COUNT; i++) { Node urlSmallFile = testUrlSmallFiles.addNode("smallFile" + i, "nt:file");
    * Node contentNode = urlSmallFile.addNode("jcr:content", "nt:resource");
    * //contentNode.setProperty("jcr:encoding", "UTF-8"); contentNode.setProperty("jcr:data", new
    * URL(URL_SMALL_FILE).openStream()); contentNode.setProperty("jcr:mimeType",
    * "application/octet-stream "); contentNode.setProperty("jcr:lastModified",
    * Calendar.getInstance()); } session.save(); endTime = System.currentTimeMillis();
    * log.info("Execution time after adding and saving: (url small)" + ((endTime - startTime) / 1000)
    * + "s"); }
    */

   @Override
   protected void tearDown() throws Exception
   {
      testBinaryValue.remove();
      root.save();
      super.tearDown();
   }
}
