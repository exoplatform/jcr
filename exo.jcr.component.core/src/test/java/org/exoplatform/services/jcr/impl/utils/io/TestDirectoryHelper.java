/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.utils.io;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TestDirectoryHelper.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestDirectoryHelper extends TestCase
{

   public void testCompressEmptyDirectory() throws Exception
   {
      File rootDir = new File("./target/emptyDir");
      rootDir.mkdir();

      File zipFile = new File("./target/compress.zip");

      try
      {
         DirectoryHelper.compressDirectory(rootDir, zipFile);
         fail("Exception should be thrown");
      }
      catch (IOException e)
      {
         // can't compress empty directory
      }
   }

   public void testCompressDirectory() throws Exception
   {
      // compress/compress.1
      File rootDir = new File("./target/compress");
      rootDir.mkdir();

      OutputStream out = new FileOutputStream(new File(rootDir, "compress.1"));
      out.write("compress.1".getBytes());
      out.close();

      // compress/a/a.1
      // compress/a/a.2
      File dirA = new File(rootDir, "a");
      dirA.mkdir();

      out = new FileOutputStream(new File(dirA, "a.1"));
      out.write("a.1".getBytes());
      out.close();

      out = new FileOutputStream(new File(dirA, "a.2"));
      out.write("a.2".getBytes());
      out.close();

      // compress/a/c/c.1
      File dirAC = new File(dirA, "c");
      dirAC.mkdir();

      out = new FileOutputStream(new File(dirAC, "c.1"));
      out.write("c.1".getBytes());
      out.close();

      // compress/b/b.1
      File dirB = new File(rootDir, "b");
      dirB.mkdir();

      out = new FileOutputStream(new File(dirB, "b.1"));
      out.write("b.1".getBytes());
      out.close();

      File zipFile = new File("./target/compressDir.zip");

      DirectoryHelper.compressDirectory(rootDir, zipFile);

      assertTrue(zipFile.exists());

      File dir = new File("./target/uncompress");
      DirectoryHelper.uncompressDirectory(zipFile, dir);

      assertFile(dir, "compress.1");
      assertFile(new File(dir, "a"), "a.1");
      assertFile(new File(dir, "a"), "a.2");
      assertFile(new File(dir, "b"), "b.1");
      assertFile(new File(new File(dir, "a"), "c"), "c.1");
   }

   public void testCompressFile() throws Exception
   {
      OutputStream out = new FileOutputStream(new File("./target/compress.file"));
      out.write("compress.file".getBytes());
      out.close();

      File zipFile = new File("./target/compressFile.zip");

      DirectoryHelper.compressDirectory(new File("./target/compress.file"), zipFile);

      assertTrue(zipFile.exists());

      File dir = new File("./target/uncompress");
      DirectoryHelper.uncompressDirectory(zipFile, dir);

      assertFile(dir, "compress.file");
   }
   
   private void assertFile(File dir, String fileName) throws Exception
   {
      byte[] buf = new byte[20];
      
      File file = new File(dir, fileName);
      assertTrue(file.exists());

      InputStream in = new FileInputStream(file);
      int len = in.read(buf);
      in.close();
      assertEquals(fileName, new String(buf, 0, len));
   }
}
