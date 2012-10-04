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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 29.07.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestCleanableFileStreamValueData.java 34664 2009-07-29 16:33:52Z pnedonosko $
 */
public class TestCleanableFileStreamValueData extends JcrImplBaseTest
{

   private static final int CLEANER_TIMEOUT = 100;

   private static final String FILE_NAME = "testFileCleaned";

   private File parentDir = new File("./target");

   private File testFile;

   private FileCleaner testCleaner;

   private CleanableFilePersistedValueData cleanableValueData;

   private static class TestSwapFile extends SwapFile
   {
      /**
       * Dummy constructor.
       */
      protected TestSwapFile(File parent, String child)
      {
         super(parent, child);
      }

      /**
       * Clean inShare for tearDown.
       * 
       */
      static void cleanShare()
      {
         inShare.clear();
      }
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testFile = new File(parentDir, FILE_NAME);
      testCleaner = new FileCleaner(CLEANER_TIMEOUT);

      SwapFile sf = SwapFile.get(parentDir, FILE_NAME);
      FileOutputStream fout = new FileOutputStream(sf);
      fout.write("testFileCleaned".getBytes());
      fout.close();
      sf.spoolDone();

      cleanableValueData = new CleanableFilePersistedValueData(1, sf, SpoolConfig.getDefaultSpoolConfig());
   }

   @Override
   protected void tearDown() throws Exception
   {
      cleanableValueData = null;

      testCleaner.halt();
      testCleaner = null;

      if (testFile.exists())
      {
         testFile.delete();
      }

      TestSwapFile.cleanShare();

      sleepAndGC();

      super.tearDown();
   }

   public void testFileCleaned() throws Exception
   {
      assertTrue(testFile.exists());
      cleanableValueData = null; // CleanableVD dies

      assertReleasedFile(testFile);
   }

   public void testSharedFileNotCleaned() throws Exception
   {
      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      CleanableFilePersistedValueData cfvd2 =
         new CleanableFilePersistedValueData(1, SwapFile.get(parentDir, FILE_NAME), SpoolConfig.getDefaultSpoolConfig());
      assertTrue(testFile.exists());

      cleanableValueData = null; // CleanableVD dies but another instance points swapped file

      // allows GC to call finalize on vd
      sleepAndGC();
      assertTrue(testFile.exists());

      // JCR-1924: fake usage to tell JDK instance is needed at this code line 
      cfvd2.getOrderNumber();

      // clean ValueData
      cfvd2 = null;
      assertReleasedFile(testFile);
   }

   public void testTransientFileNotCleaned() throws Exception
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      TransientValueData trvd = new TransientValueData(false);
      trvd.delegate(cleanableValueData);

      trvd = null; // TransientVD dies

      // allows GC to call finalize on vd
      sleepAndGC();
      assertTrue(testFile.exists()); // but Swapped CleanableVD lives and uses the file
   }

   public void testTransientFileCleaned() throws Exception
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      TransientValueData trvd = new TransientValueData(false);
      trvd.delegate(cleanableValueData);
      
      assertTrue(testFile.exists());

      cleanableValueData = null; // CleanableVD dies but TransientVD still uses swapped file

      sleepAndGC();
      assertTrue(testFile.exists());

      // JCR-1924: fake usage to tell JDK instance is needed at this code line 
      trvd.getOrderNumber();

      trvd = null; // TransientVD dies
      assertReleasedFile(testFile);
   }

   public void testTransientSharedFileCleaned() throws Exception
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      // file shared with TransientVD
      TransientValueData trvd = new TransientValueData(false);
      trvd.delegate(cleanableValueData);

      assertTrue(testFile.exists());

      // 1st CleanableVD die
      cleanableValueData = null;

      sleepAndGC();

      // file shared with third CleanableVD, i.e. file still exists (aquired by TransientVD)
      CleanableFilePersistedValueData cfvd2 =
         new CleanableFilePersistedValueData(1, SwapFile.get(parentDir, FILE_NAME), SpoolConfig.getDefaultSpoolConfig());
      assertTrue(testFile.exists());

      // JCR-1924: fake usage to tell JDK instance is needed at this code line 
      trvd.getOrderNumber();

      trvd = null; // TransientVD dies

      sleepAndGC();
      assertTrue(testFile.exists()); // still exists, aquired by 2nd CleanableVD

      // JCR-1924: fake usage to tell JDK instance is needed at this code line 
      cfvd2.getOrderNumber();

      cfvd2 = null; // 2nd CleanableVD dies
      assertReleasedFile(testFile);
   }

   private void sleepAndGC() throws Exception
   {
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();
   }

   private void assertReleasedFile(File file) throws Exception
   {
      long purgeStartTime = System.currentTimeMillis();
      while (file.exists() && (System.currentTimeMillis() - purgeStartTime < 2 * 60 * 1000))
      {
         System.gc();
         try
         {
            Thread.sleep(500);
         }
         catch (InterruptedException e)
         {
         }
      }

      assertFalse(file.exists()); // file released and deleted
   }
}
