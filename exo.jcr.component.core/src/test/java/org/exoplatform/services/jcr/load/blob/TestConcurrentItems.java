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
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.load.blob.thread.CreateThread;
import org.exoplatform.services.jcr.load.blob.thread.DeleteThread;
import org.exoplatform.services.jcr.load.blob.thread.NtFileCreatorThread;
import org.exoplatform.services.jcr.load.blob.thread.ReadThread;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 19.10.2006 Subjetc of the test it's to test BLOB data storing in eXo JCR with/without
 * swap/binary.temp storages in concurent environment. Also can be used for test eXo JCR without
 * values storage.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestConcurrentItems.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestConcurrentItems extends JcrAPIBaseTest
{

   private Node testBinaryValue = null;

   // some lines for use in test
   // public final static String LOCAL_BIG_FILE =
   // "src/test/resources/BigFile.zip";
   // public final static String URL_BIG_MEDIA_FILE=
   // "ftp://exoua.dnsalias.net/pub/video/Lost.Season3.Preview.rus.avi";
   // public final static String LOCAL_SMALL_FILE =
   // "src/test/resources/SmallFile.zip";
   // public final static byte[] LOCAL_SMALL_FILE_DATA =
   // LOCAL_SMALL_FILE.getBytes();

   public final static String TEST_ROOT = "blob_test";

   public static String TEST_FILE = null;

   public static long TEST_FILE_SIZE;

   public static Set<String> consumedNodes = Collections.synchronizedSet(new HashSet<String>());

   private File testFile = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      Session testSession = repository.login(this.credentials, "ws");
      testBinaryValue = testSession.getRootNode().addNode(TEST_ROOT);
      testSession.save();

      int dataSize = 0;
      if (TEST_FILE == null)
      {
         // create test file
         testFile = createBLOBTempFile(1024);
         dataSize = (int)testFile.length();
         TEST_FILE = testFile.getAbsolutePath();
      }
      else
      {
         // calc stream size
         byte[] buff = new byte[1024 * 4];
         int read = 0;
         // InputStream dataStream = new URL(TEST_FILE).openStream();
         InputStream dataStream = PrivilegedFileHelper.fileInputStream(TEST_FILE);
         while ((read = dataStream.read(buff)) >= 0)
         {
            dataSize += read;
         }
         dataStream.close();
      }
      TEST_FILE_SIZE = dataSize;
   }

   @Override
   protected void tearDown() throws Exception
   {
      log.info("Tear down begin...");
      try
      {
         if (testFile != null)
            testFile.delete();
      }
      catch (Throwable e)
      {
         log.error("Temp test file error of delete: " + e.getMessage(), e);
      }
      finally
      {
         log.info("Remove test root...");
         testBinaryValue.remove();
         testBinaryValue.getSession().save();
         log.info("Remove test root done");
         super.tearDown();
         log.info("Tear down done");
      }
   }

   public void _testReadSame() throws Exception
   {
      // creators
      Session csession = repository.login(this.credentials, "ws1");
      String nodeName = IdGenerator.generate();
      InputStream dataStream = null;
      try
      {
         Node testRoot = csession.getRootNode().getNode(TestConcurrentItems.TEST_ROOT);
         Node ntFile = testRoot.addNode(nodeName, "nt:file");
         Node contentNode = ntFile.addNode("jcr:content", "nt:resource");
         dataStream = PrivilegedFileHelper.fileInputStream(TestConcurrentItems.TEST_FILE);
         PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", dataStream);
         contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
         csession.save();
         log.debug("Create node: " + ntFile.getPath() + ", data: " + data.getInternalIdentifier());
      }
      finally
      {
         if (dataStream != null)
            try
            {
               dataStream.close();
            }
            catch (IOException e)
            {
               log.error("Stream read error: " + e.getMessage(), e);
            }
      }

      List<ReadThread> readers = new ArrayList<ReadThread>();

      log.info("Begin readers...");
      for (int i = 0; i < 10; i++)
      {
         ReadThread readed = new ReadThread(repository.login(this.credentials, "ws1"));
         readed.start();
         readers.add(readed);
         try
         {
            Thread.sleep(100);
         }
         catch (InterruptedException e)
         {
            log.error("Start reader. Sleep error: " + e.getMessage(), e);
         }
      }

      // wait cycles, for process visualisation
      // 360 - 60 min
      // 4320 - 12 hours
      int cycles = 5;
      while (cycles >= 0)
      {
         Thread.yield();
         try
         {
            Thread.sleep(10000);
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Sleep error: " + e.getMessage(), e);
         }
         log.info("<<<<<<<<<<<<<<<<<<<< Cycle " + cycles + " >>>>>>>>>>>>>>>>>>>>>");
         cycles--;
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Stopping >>>>>>>>>>>>>>>>>>>>>");

      for (ReadThread reader : readers)
      {
         try
         {
            reader.testStop();
            reader.join();
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Readed stop error: " + e.getMessage(), e);
         }
      }

      csession.logout(); // release the session

      log.info("<<<<<<<<<<<<<<<<<<<< Stopped >>>>>>>>>>>>>>>>>>>>>");
      try
      {
         System.gc();
         Thread.yield();
         Thread.sleep(5000);
      }
      catch (InterruptedException e)
      {
         log.error("Test stop. Sleep error: " + e.getMessage(), e);
      }
   }

   public void _testReadWriteSet() throws Exception
   {
      // creators
      CreateThread creator = new CreateThread(repository.login(this.credentials, "ws1"));
      creator.start();
      try
      {
         log.info("Wait 20 sec. for CreateThread");
         Thread.sleep(20000); // 30 sec.
      }
      catch (InterruptedException e)
      {
         log.error("Creator wait. Sleep error: " + e.getMessage(), e);
      }

      List<ReadThread> readers = new ArrayList<ReadThread>();

      log.info("Begin readers...");
      for (int i = 0; i < 5; i++)
      {
         ReadThread readed =
            new ReadThread(
               repository
                  .login(
                     this.credentials /*
                                                                                                                                        * session.getCredentials(
                                                                                                                                        * )
                                                                                                                                        */,
                     "ws1"));
         readed.start();
         readers.add(readed);
         try
         {
            Thread.sleep(5000);
         }
         catch (InterruptedException e)
         {
            log.error("Start reader. Sleep error: " + e.getMessage(), e);
         }
      }

      log.info("Begin cleaner...");
      DeleteThread cleaner = new DeleteThread(repository.login(this.credentials /*
                                                                                                         * session.getCredentials
                                                                                                         * ()
                                                                                                         */, "ws1"));
      cleaner.start();

      log.info("<<<<<<<<<<<<<<<<<<<< Wait cycle >>>>>>>>>>>>>>>>>>>>>");
      // 360 - 60 min
      // 4320 - 12 hours
      int cycles = 180; // 5min
      while (cycles >= 0)
      {
         Thread.yield();
         try
         {
            Thread.sleep(10000);
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Sleep error: " + e.getMessage(), e);
         }
         log.info("<<<<<<<<<<<<<<<<<<<< Cycle " + cycles + " >>>>>>>>>>>>>>>>>>>>>");
         cycles--;
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Stopping >>>>>>>>>>>>>>>>>>>>>");

      for (ReadThread reader : readers)
      {
         try
         {
            reader.testStop();
            reader.join(3000);
            Thread.yield();
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Readed stop error: " + e.getMessage(), e);
         }
      }

      try
      {
         creator.testStop();
         creator.join();
         Thread.yield();
      }
      catch (InterruptedException e)
      {
         log.error("Test lifecycle. Creator stop error: " + e.getMessage(), e);
      }

      try
      {
         cleaner.testStop();
         cleaner.join();
         Thread.yield();
      }
      catch (InterruptedException e)
      {
         log.error("Test lifecycle. Cleaner stop error: " + e.getMessage(), e);
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Stopped >>>>>>>>>>>>>>>>>>>>>");
      try
      {
         Thread.sleep(15000);
      }
      catch (InterruptedException e)
      {
         log.error("Test stop. Sleep error: " + e.getMessage(), e);
      }
   }

   public void testAddNtFiles() throws Exception
   {
      List<NtFileCreatorThread> creators = new ArrayList<NtFileCreatorThread>();

      log.info("Begin creators...");
      for (int i = 0; i < 100; i++)
      {
         Session creatorSession = repository.login(this.credentials, "ws");
         Node root = creatorSession.getRootNode().getNode(TestConcurrentItems.TEST_ROOT);

         String testRootName = "root-" + IdGenerator.generate();
         root.addNode(testRootName);
         creatorSession.save();

         NtFileCreatorThread creator = new NtFileCreatorThread(creatorSession, testRootName);
         creator.start();
         creators.add(creator);
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
            log.error("Start creator. Sleep error: " + e.getMessage(), e);
         }
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Wait cycle >>>>>>>>>>>>>>>>>>>>>");
      // 360 - 60 min
      // 4320 - 12 hours
      int cycles = 180; // 5min
      while (cycles >= 0)
      {
         Thread.yield();
         try
         {
            Thread.sleep(10000);
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Sleep error: " + e.getMessage(), e);
         }
         log.info("<<<<<<<<<<<<<<<<<<<< Cycle " + cycles + " >>>>>>>>>>>>>>>>>>>>>");
         cycles--;
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Stopping >>>>>>>>>>>>>>>>>>>>>");

      for (NtFileCreatorThread reader : creators)
      {
         try
         {
            reader.testStop();
            reader.join();
            Thread.yield();
         }
         catch (InterruptedException e)
         {
            log.error("Test lifecycle. Readed stop error: " + e.getMessage(), e);
         }
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Stopped >>>>>>>>>>>>>>>>>>>>>");

      final int waitSec = 5; // 30 sec.

      log.info("<<<<<<<<<<<<<<<<<<<< Wait " + waitSec + "sec. >>>>>>>>>>>>>>>>>>>>>");

      Thread waiter = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               Thread.sleep(waitSec * 1000);
            }
            catch (Throwable e)
            {
               System.err.println("Waiter error " + e);
            }
         }
      };

      try
      {
         waiter.start();
         waiter.join();
      }
      catch (InterruptedException e)
      {
         log.error("Wait error: " + e.getMessage(), e);
      }

      log.info("<<<<<<<<<<<<<<<<<<<< Done >>>>>>>>>>>>>>>>>>>>>");
   }

   public void _testAddNtBig() throws Exception
   {
      // creators
      Session csession = repository.login(this.credentials, "ws1");
      String nodeName = IdGenerator.generate();
      InputStream dataStream = null;
      try
      {
         Node testRoot = csession.getRootNode().getNode(TestConcurrentItems.TEST_ROOT);
         Node ntFile = testRoot.addNode(nodeName, "nt:file");
         Node contentNode = ntFile.addNode("jcr:content", "nt:resource");
         dataStream = PrivilegedFileHelper.fileInputStream(TEST_FILE);
         PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", dataStream);
         contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
         csession.save();
         log.info("Create node: " + ntFile.getPath() + ", data: " + data.getInternalIdentifier());
      }
      finally
      {
         if (dataStream != null)
            try
            {
               dataStream.close();
            }
            catch (IOException e)
            {
               log.error("Stream read error: " + e.getMessage(), e);
            }
      }
   }
}
