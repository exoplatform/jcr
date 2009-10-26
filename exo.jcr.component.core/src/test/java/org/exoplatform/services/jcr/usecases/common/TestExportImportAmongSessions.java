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
package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestExportImportAmongSessions.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestExportImportAmongSessions extends BaseUsecasesTest
{

   static private String TEST_NODE = "testNode";

   static private String TEST_NTFILE = "nt file 1";

   static private byte[] TEST_BINARY_CONTENT = "Some text as binary value".getBytes();

   public void testExportImportDocView() throws Exception
   {

      Session session1 = repository.getSystemSession(repository.getSystemWorkspaceName());
      Node testNode = session1.getRootNode().addNode(TEST_NODE);
      Node testNtFile = testNode.addNode(TEST_NTFILE, "nt:file");

      Node testNtFileContent = testNtFile.addNode("jcr:content", "nt:resource");
      testNtFileContent.setProperty("jcr:encoding", "UTF-8");
      testNtFileContent.setProperty("jcr:lastModified", Calendar.getInstance());
      testNtFileContent.setProperty("jcr:mimeType", "text/html");
      testNtFileContent.setProperty("jcr:data", new ByteArrayInputStream(TEST_BINARY_CONTENT));
      session1.save();

      File outputFile = File.createTempFile("jcr_bin_test-", ".tmp");
      outputFile.deleteOnExit();

      session1.exportDocumentView(testNode.getPath(), new FileOutputStream(outputFile), false, false);

      testNode.remove();
      session1.save();

      try
      {
         session1.importXML("/", new FileInputStream(outputFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
         session1.save();

         testNode = session1.getRootNode().getNode(TEST_NODE);
         Node ntFile = testNode.getNode(TEST_NTFILE);
         InputStream storedData = ntFile.getProperty("jcr:content/jcr:data").getStream();
         assertTrue("AFTER EXPORT/IMPORT. Binary content must be same", checkBinaryEquals(new ByteArrayInputStream(
            TEST_BINARY_CONTENT), storedData));
      }
      catch (RepositoryException e)
      {
         fail("Node import failed: " + e);
      }
   }

   public void testExportImportDocViewCrossSession() throws Exception
   {

      Session session1 = repository.getSystemSession(repository.getSystemWorkspaceName());
      Node testNode = session1.getRootNode().addNode(TEST_NODE);
      Node testNtFile = testNode.addNode(TEST_NTFILE, "nt:file");
      // testNtFile.setProperty("jcr:created", Calendar.getInstance());

      Node testNtFileContent = testNtFile.addNode("jcr:content", "nt:resource");
      // testNtFileContent.setProperty("jcr:uuid", testNtFileContent.getUUID());
      testNtFileContent.setProperty("jcr:encoding", "UTF-8");
      testNtFileContent.setProperty("jcr:lastModified", Calendar.getInstance());
      testNtFileContent.setProperty("jcr:mimeType", "text/html");
      InputStream etalonData = new ByteArrayInputStream("Some text as binary value".getBytes());
      testNtFileContent.setProperty("jcr:data", etalonData);
      session1.save();

      Session session2 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), repository.getSystemWorkspaceName());
      testNode = session1.getRootNode().getNode(TEST_NODE);
      Node ntFile = testNode.getNode(TEST_NTFILE);
      InputStream storedData = ntFile.getProperty("jcr:content/jcr:data").getStream();
      assertTrue("BEFORE EXPORT/IMPORT. Binary content must be same", checkBinaryEquals(etalonData, storedData));

      File outputFile = File.createTempFile("jcr_bin_test", ".tmp");
      outputFile.deleteOnExit();

      session2.exportDocumentView(testNode.getPath(), new FileOutputStream(outputFile), false, false);

      testNode.remove();
      session2.save();

      try
      {
         session1.importXML("/", new FileInputStream(outputFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

         testNode = session1.getRootNode().getNode(TEST_NODE);
         ntFile = testNode.getNode(TEST_NTFILE);
         storedData = ntFile.getProperty("jcr:content/jcr:data").getStream();
         assertTrue("AFTER EXPORT/IMPORT. Binary content must be same", checkBinaryEquals(new ByteArrayInputStream(
            TEST_BINARY_CONTENT), storedData));
      }
      catch (RepositoryException e)
      {
         fail("Node import (without save of result) failed. : " + e);
      }

      try
      {
         session1.save();

         testNode = session1.getRootNode().getNode(TEST_NODE);
         ntFile = testNode.getNode(TEST_NTFILE);
         storedData = ntFile.getProperty("jcr:content/jcr:data").getStream();
         assertTrue("AFTER EXPORT/IMPORT AFTER SAVE. Binary content must be same", checkBinaryEquals(
            new ByteArrayInputStream(TEST_BINARY_CONTENT), storedData));
      }
      catch (RepositoryException e)
      {
         fail("Node import (with save of result) failed. : " + e);
      }
   }

   boolean checkBinaryEquals(InputStream etalon, InputStream subject) throws Exception
   {
      try
      {
         boolean continueLoop = etalon.available() > 0 && subject.available() > 0;
         int etalonCounter = 0;
         int subjecCounter = 0;
         while (continueLoop)
         {
            int etalonByte = etalon.read();
            int subjectByte = subject.read();
            if (etalonByte >= 0 && subjectByte >= 0)
            {
               if ((etalonByte & 0xF0) != (subjectByte & 0xF0))
               {
                  return false;
               }
               continueLoop = etalon.available() > 0 && subject.available() > 0;
               etalonCounter++;
               subjecCounter++;
            }
            else
            {
               return false;
            }
         }
         return etalonCounter == subjecCounter;
      }
      catch (IOException e)
      {
         log.error("Error compare buinary streams", e);
         return false;
      }
   }
}
