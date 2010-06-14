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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestExportBigFiles.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestExportBigFiles extends JcrAPIBaseTest
{
   /**
    * Testing SysView import - export
    * 
    * @throws Exception
    */
   public void testBigExportSysView() throws Exception
   {
      String TEST_FILE = PrivilegedFileHelper.getAbsolutePath(createBLOBTempFile(1024 * 5));// 5M
      Node testLocalBigFiles = root.addNode("testLocalBigFiles");

      // add file to repository
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start
      Node localBigFile = testLocalBigFiles.addNode("bigFile" + 1, "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      // contentNode.setProperty("jcr:encoding", "UTF-8");
      InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE);
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      is.close();
      System.out.println("Data is set: " + TEST_FILE);
      // contentNode.setProperty("jcr:mimeType", "video/avi");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();

      System.out.println("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      // Exporting repository content
      File file = PrivilegedFileHelper.createTempFile("tesSysExport", ".xml");

      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(file));
      session.exportSystemView(testLocalBigFiles.getPath(), bufferedOutputStream, false, false);
      bufferedOutputStream.flush();
      bufferedOutputStream.close();
      assertTrue(PrivilegedFileHelper.length(file) > 0);

      // removing source node
      testLocalBigFiles.remove();
      session.save();

      BufferedInputStream bufferedInputStream = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(file));

      // importing content
      session.importXML(root.getPath(), bufferedInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      session.save();

      Node n1 = root.getNode("testLocalBigFiles");
      Node lbf = n1.getNode("bigFile" + 1);
      Node content = lbf.getNode("jcr:content");

      // comparing with source file
      compareStream(new BufferedInputStream(PrivilegedFileHelper.fileInputStream(TEST_FILE)), content.getProperty(
         "jcr:data").getStream());

      n1.remove();
      session.save();
      PrivilegedFileHelper.deleteOnExit(file);
      PrivilegedFileHelper.delete(file);

   }

   /**
    * Testing SysView import - export
    * 
    * @throws Exception
    */
   public void testBigImportExportDocView() throws Exception
   {
      String TEST_FILE2 = PrivilegedFileHelper.getAbsolutePath(createBLOBTempFile(1024 * 5)); // 5M
      Node testLocalBigFiles = root.addNode("testDocView");

      // add file to repository
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start
      Node localBigFile = testLocalBigFiles.addNode("bigFile" + 1, "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      // contentNode.setProperty("jcr:encoding", "UTF-8");
      InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE2);
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      is.close();
      System.out.println("Data is set: " + TEST_FILE2);
      // contentNode.setProperty("jcr:mimeType", "video/avi");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();

      System.out.println("Saved: " + TEST_FILE2 + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      // Exporting repository content
      File file = PrivilegedFileHelper.createTempFile("tesDocExport", ".xml");

      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(file));
      session.exportDocumentView(testLocalBigFiles.getPath(), bufferedOutputStream, false, false);
      bufferedOutputStream.flush();
      bufferedOutputStream.close();
      assertTrue(PrivilegedFileHelper.length(file) > 0);

      // removing source node
      testLocalBigFiles.remove();
      session.save();

      BufferedInputStream bufferedInputStream = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(file));

      // importing content
      session.importXML(root.getPath(), bufferedInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      session.save();

      Node n1 = root.getNode("testDocView");
      Node lbf = n1.getNode("bigFile" + 1);
      Node content = lbf.getNode("jcr:content");

      // comparing with source file
      compareStream(new BufferedInputStream(PrivilegedFileHelper.fileInputStream(TEST_FILE2)), content.getProperty(
         "jcr:data").getStream());

      n1.remove();
      session.save();
      PrivilegedFileHelper.deleteOnExit(file);
      PrivilegedFileHelper.delete(file);
   }

   public void testIEPdfFiles() throws Exception
   {
      //
      // 300 MB creating file
      String TEST_FILE = "D:/Dev/DOC/jsr170-1.0.pdf";
      Node testLocalBigFiles = root.addNode("testLocalBigFiles");

      // add file to repository
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start
      Node localBigFile = testLocalBigFiles.addNode("bigFile" + 1, "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      // contentNode.setProperty("jcr:encoding", "UTF-8");
      InputStream is = PrivilegedFileHelper.fileInputStream(TEST_FILE);
      contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      is.close();
      System.out.println("Data is set: " + TEST_FILE);
      // contentNode.setProperty("jcr:mimeType", "video/avi");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();

      System.out.println("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      endTime = System.currentTimeMillis();
      log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      // Exporting repository content
      File file = PrivilegedFileHelper.createTempFile("tesSysExport", ".xml");

      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(file));
      session.exportSystemView(testLocalBigFiles.getPath(), bufferedOutputStream, false, false);
      bufferedOutputStream.flush();
      bufferedOutputStream.close();
      assertTrue(PrivilegedFileHelper.length(file) > 0);

      // removing source node
      testLocalBigFiles.remove();
      session.save();

      BufferedInputStream bufferedInputStream = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(file));

      // importing content
      session.importXML(root.getPath(), bufferedInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      session.save();

      Node n1 = root.getNode("testLocalBigFiles");
      Node lbf = n1.getNode("bigFile" + 1);
      Node content = lbf.getNode("jcr:content");

      // comparing with source file
      compareStream(new BufferedInputStream(PrivilegedFileHelper.fileInputStream(TEST_FILE)), content.getProperty(
         "jcr:data").getStream());

      n1.remove();
      session.save();
      PrivilegedFileHelper.deleteOnExit(file);
      PrivilegedFileHelper.delete(file);
   }

   public void testRandomSizeExportImportSysView() throws Exception
   {
      final int FILES_COUNT = 100;

      List<String> fileList = new ArrayList<String>();
      Random random = new Random();

      for (int i = 0; i < FILES_COUNT; i++)
      {
         fileList.add(PrivilegedFileHelper.getAbsolutePath(createBLOBTempFile(random.nextInt(1024 * 1024))));
      }
      Node testLocalBigFiles = root.addNode("testLocalBigFiles");

      // add file to repository
      long startTime, endTime;
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String TEST_FILE = fileList.get(i);
         startTime = System.currentTimeMillis(); // to get the time of start
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
         session.save();
         System.out.println("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
         endTime = System.currentTimeMillis();
         log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      }

      // Exporting repository content
      File file = PrivilegedFileHelper.createTempFile("tesSysExport", ".xml");

      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(file));
      session.exportSystemView(testLocalBigFiles.getPath(), bufferedOutputStream, false, false);
      bufferedOutputStream.flush();
      bufferedOutputStream.close();
      assertTrue(PrivilegedFileHelper.length(file) > 0);

      // removing source node
      testLocalBigFiles.remove();
      session.save();

      BufferedInputStream bufferedInputStream = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(file));

      // importing content
      session.importXML(root.getPath(), bufferedInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);

      session.save();

      Node n1 = root.getNode("testLocalBigFiles");
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String TEST_FILE = fileList.get(i);
         Node lbf = n1.getNode("bigFile" + i);
         Node content = lbf.getNode("jcr:content");
         // comparing with source file
         compareStream(new BufferedInputStream(PrivilegedFileHelper.fileInputStream(TEST_FILE)), content.getProperty(
            "jcr:data").getStream());
      }
      n1.remove();
      session.save();
      PrivilegedFileHelper.deleteOnExit(file);
      PrivilegedFileHelper.delete(file);

   }

   @Override
   protected File createBLOBTempFile(int sizeInb) throws IOException
   {
      // create test file
      byte[] data = new byte[1024]; // 1Kb

      File testFile = PrivilegedFileHelper.createTempFile("exportImportFileTest", ".tmp");
      FileOutputStream tempOut = PrivilegedFileHelper.fileOutputStream(testFile);
      Random random = new Random();

      for (int i = 0; i < sizeInb; i += 1024)
      {
         if (i + 1024 > sizeInb)
         {
            byte[] rest = new byte[sizeInb - i];
            random.nextBytes(rest);
            tempOut.write(rest);
            continue;
         }
         random.nextBytes(data);
         tempOut.write(data);
      }
      tempOut.close();
      PrivilegedFileHelper.deleteOnExit(testFile);
      log.info("Temp file created: " + PrivilegedFileHelper.getAbsolutePath(testFile) + " size: "
         + PrivilegedFileHelper.length(testFile));
      return testFile;
   }
}
