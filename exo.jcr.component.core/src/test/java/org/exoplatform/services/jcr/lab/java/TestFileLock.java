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
package org.exoplatform.services.jcr.lab.java;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 31.03.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestFileLock.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class TestFileLock extends TestCase
{

   public void _testSameJVMlLock() throws IOException, InterruptedException
   {

      File f = new File("\\\\storm\\public\\file1.tmp");
      f.createNewFile();
      FileOutputStream fout = new FileOutputStream(f);
      FileChannel fc = fout.getChannel();

      ByteBuffer buff = ByteBuffer.wrap("test-file1".getBytes());
      fc.write(buff);

      fc.close();

      // reopen
      FileInputStream fin = new FileInputStream(f);
      fc = fin.getChannel();

      byte[] b = new byte[256];
      ByteBuffer dst = ByteBuffer.wrap(b);
      int res = fc.read(dst);

      if (res > 0)
         System.out.println(new String(b, 0, res));

      // lock file
      fout = new FileOutputStream(f);
      fc = fout.getChannel();
      FileLock lock = fc.lock();

      assertTrue(lock.isValid());

      // check another lock
      Thread another = new Thread()
      {
         public void run()
         {
            try
            {
               Thread.sleep(25);
               FileOutputStream fout1 = new FileOutputStream(new File("\\\\storm\\public\\file1.tmp"));
               FileChannel fc1 = fout1.getChannel();
               try
               {
                  FileLock lock1 = fc1.lock();
                  System.out.println("Another locked " + System.currentTimeMillis());
                  Thread.sleep(10000);
                  lock1.release();
                  System.out.println("Another released " + System.currentTimeMillis());
               }
               finally
               {
                  fc1.close();
               }
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      };
      another.start();
      System.out.println("Another started " + System.currentTimeMillis());
      // ////////////////////

      Thread.sleep(2000);
      lock.release();
      fc.close();

      // print final content
      fin = new FileInputStream(f);
      fc = fin.getChannel();

      res = -1;
      dst = ByteBuffer.wrap(b);
      try
      {
         res = fc.read(dst);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      if (res > 0)
         System.out.println(new String(b, 0, res));
   }

   public void _testDifferentJVMLock() throws IOException, InterruptedException
   {

      final long timeout = Long.valueOf(System.getProperty("exo.jcr.lab.testFileLockTimeout", "20000"));

      // File f = new File("\\\\storm\\public\\file2.tmp");
      File f = new File("D:\\tmp\\file2.tmp");

      System.out.println("Try open file for write " + System.currentTimeMillis());
      FileOutputStream fout = new FileOutputStream(f);
      FileChannel fc = fout.getChannel();
      System.out.println("Try lock file " + System.currentTimeMillis());
      FileLock lock = fc.lock();
      System.out.println("File locked " + System.currentTimeMillis());
      assertTrue(lock.isValid());

      Thread.sleep(timeout);

      ByteBuffer buff = ByteBuffer.wrap("test-file2".getBytes());
      fc.write(buff);
      // release and close
      lock.release();
      fc.close();
      System.out.println("File written and closed " + System.currentTimeMillis());

      // reopen for read
      Thread.sleep(timeout);
      System.out.println("Try read file " + System.currentTimeMillis());
      FileInputStream fin = new FileInputStream(f);
      fc = fin.getChannel();
      System.out.println("Try shared lock " + System.currentTimeMillis());
      FileLock shlock = fc.lock(0, fc.size(), true);
      System.out.println("File locked shared " + System.currentTimeMillis());

      byte[] b = new byte[256];
      ByteBuffer dst = ByteBuffer.wrap(b);
      int res = fc.read(dst);
      if (res > 0)
         System.out.println(new String(b, 0, res));
      shlock.release();
      System.out.println("Read file OK " + System.currentTimeMillis());
      fc.close();

      // lock file for write
      Thread.sleep(timeout);
      System.out.println("Try open file for write " + System.currentTimeMillis());
      fout = new FileOutputStream(f);
      fc = fout.getChannel();
      System.out.println("Try lock file " + System.currentTimeMillis());
      lock = fc.lock();
      System.out.println("File locked " + System.currentTimeMillis());
      assertTrue(lock.isValid());

      buff = ByteBuffer.wrap("test-file2 new content".getBytes());
      fc.write(buff);
      Thread.sleep(timeout);

      lock.release();
      System.out.println("File released " + System.currentTimeMillis());

      Thread.sleep(timeout);
      fc.close();
      System.out.println("File closed " + System.currentTimeMillis());

      // print final content
      Thread.sleep(timeout);
      System.out.println("Try read file " + System.currentTimeMillis());
      fin = new FileInputStream(f);
      fc = fin.getChannel();
      System.out.println("Try shared lock " + System.currentTimeMillis());
      shlock = fc.lock(0, fc.size(), true);
      System.out.println("File locked shared " + System.currentTimeMillis());

      b = new byte[256];
      dst = ByteBuffer.wrap(b);
      res = -1;
      if (res > 0)
         System.out.println(new String(b, 0, res));
      shlock.release();
      System.out.println("Read file OK " + System.currentTimeMillis());
      fc.close();
   }

   public void _testDifferentJVMReadLock() throws IOException, InterruptedException
   {

      final long timeout = Long.valueOf(System.getProperty("exo.jcr.lab.testFileLockTimeout", "20000"));

      // File f = new File("\\\\storm\\public\\file3.tmp");
      File f = new File("D:\\tmp\\file3.tmp");

      // write new content
      System.out.println("Try open file for write " + System.currentTimeMillis());
      FileOutputStream fout = new FileOutputStream(f);
      FileChannel fc = fout.getChannel();
      System.out.println("Try lock file " + System.currentTimeMillis());
      FileLock lock = fc.lock();
      System.out.println("File locked " + System.currentTimeMillis());
      assertTrue(lock.isValid());

      ByteBuffer buff = ByteBuffer.wrap("test-file3".getBytes());
      fc.write(buff);
      lock.release();
      fc.close();
      System.out.println("File prepared " + System.currentTimeMillis());

      // reopen for read
      Thread.sleep(timeout);
      System.out.println("Try read file " + System.currentTimeMillis());
      FileInputStream fin = new FileInputStream(f);
      fc = fin.getChannel();
      System.out.println("Try shared lock " + System.currentTimeMillis());
      FileLock shlock = fc.lock(0, fc.size(), true);
      System.out.println("File locked shared " + System.currentTimeMillis());

      byte[] b = new byte[256];
      ByteBuffer dst = ByteBuffer.wrap(b);
      int res = fc.read(dst);
      if (res > 0)
         System.out.println(new String(b, 0, res));

      System.out.println("Read file OK " + System.currentTimeMillis());
      Thread.sleep(timeout);
      shlock.release();
      System.out.println("File shared lock released " + System.currentTimeMillis());
      Thread.sleep(timeout);
      fc.close();
      System.out.println("File closed " + System.currentTimeMillis());
   }

   public void testInputStreamLock() throws IOException, InterruptedException
   {

      // File f = new File("\\\\storm\\public\\file3.tmp");
      File f = new File("D:\\tmp\\file4.tmp");

      // write new content
      System.out.println("Try open file for read " + System.currentTimeMillis());
      FileInputStream fin = new FileInputStream(f);
      FileChannel fc = fin.getChannel();
      System.out.println("Try lock file " + System.currentTimeMillis());
      fin.read();
      fin.close();
   }
}
