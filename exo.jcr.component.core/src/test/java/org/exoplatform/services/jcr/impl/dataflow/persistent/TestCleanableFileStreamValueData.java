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
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.jcr.RepositoryException;

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

   private static final int CLEANER_TIMEOUT = 4000; // 4sec

   private static final String FILE_NAME = "testFileCleaned";

   private File parentDir = new File("./target");

   private File testFile = new File(parentDir, FILE_NAME);

   private FileCleaner testCleaner;

   private CleanableFilePersistedValueData cleanableValueData;

   private static class TestSwapFile extends SwapFile
   {
      /**
       * Dummy constructor.
       * 
       * @param parent
       *          Fiel
       * @param child
       *          String
       */
      protected TestSwapFile(File parent, String child,FileCleaner cleaner)
      {
         super(parent, child,cleaner);
      }

      /**
       * Clean inShare for tearDown.
       * 
       */
      static void cleanShare()
      {
         CURRENT_SWAP_FILES.clear();
      }
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testCleaner = new FileCleaner(CLEANER_TIMEOUT);

      SwapFile sf = SwapFile.get(parentDir, FILE_NAME,testCleaner);
      FileOutputStream fout = new FileOutputStream(sf);
      fout.write("testFileCleaned".getBytes());
      fout.close();
      sf.spoolDone();

      cleanableValueData = new CleanableFilePersistedValueData(1, sf, testCleaner);
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

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      super.tearDown();
   }

   public void testFileCleaned() throws InterruptedException
   {

      assertTrue(testFile.exists());

      cleanableValueData = null; // CleanableVD dies

      // allows GC to call finalize on vd
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertFalse(testFile.exists()); // file released and deleted
   }

   public void testSharedFileNotCleaned() throws InterruptedException, IOException
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      CleanableFilePersistedValueData cfvd2 =
         new CleanableFilePersistedValueData(1, SwapFile.get(parentDir, FILE_NAME,testCleaner), testCleaner);
      assertTrue(testFile.exists());

      cleanableValueData = null; // CleanableVD dies but another instance points swapped file

      // allows GC to call finalize on vd
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertTrue(testFile.exists());

      // clean ValueData
      cfvd2 = null;

      // allows GC to call finalize on vd
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertFalse(testFile.exists());
   }

   public void testTransientFileNotCleaned() throws InterruptedException, IOException, RepositoryException
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      TransientValueData trvd = cleanableValueData.createTransientCopy();
      assertTrue(testFile.exists());

      trvd = null; // TransientVD dies

      // allows GC to call finalize on vd
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertTrue(testFile.exists()); // but Swapped CleanableVD lives and uses the file
   }

   public void testTransientFileCleaned() throws InterruptedException, IOException, RepositoryException
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      TransientValueData trvd = cleanableValueData.createTransientCopy();
      assertTrue(testFile.exists());

      cleanableValueData = null; // CleanableVD dies but TransientVD still uses swapped file

      // allows GC to work
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertTrue(testFile.exists());

      trvd = null; // TransientVD dies

      // allows GC to call finalize on vd
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertFalse(testFile.exists()); // swapped file deleted
   }

   public void testTransientSharedFileCleaned() throws InterruptedException, IOException, RepositoryException
   {

      assertTrue(testFile.exists());

      System.gc();
      Thread.sleep(CLEANER_TIMEOUT / 2);

      // file shared with TransientVD
      TransientValueData trvd = cleanableValueData.createTransientCopy();

      assertTrue(testFile.exists());

      // 1st CleanableVD die
      cleanableValueData = null;

      // allows GC to work
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      // file shared with third CleanableVD, i.e. file still exists (aquired by TransientVD)
      CleanableFilePersistedValueData cfvd2 =
         new CleanableFilePersistedValueData(1, SwapFile.get(parentDir, FILE_NAME,testCleaner), testCleaner);
      assertTrue(testFile.exists());

      trvd = null; // TransientVD dies

      // allows GC to work
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertTrue(testFile.exists()); // still exists, aquired by 2nd CleanableVD

      cfvd2 = null; // 2nd CleanableVD dies

      // allows GC to work
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertFalse(testFile.exists()); // file should be deleted
   }

}
