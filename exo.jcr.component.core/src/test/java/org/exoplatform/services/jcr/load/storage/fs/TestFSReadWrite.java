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
package org.exoplatform.services.jcr.load.storage.fs;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.util.SIDGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by The eXo Platform SAS 10.07.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestFSReadWrite.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestFSReadWrite extends TestCase
{

   private static Logger log = Logger.getLogger("exo.jcr.component.core.TestFSReadWrite");

   public static final int FILES_COUNT = 50000;

   protected File testRoot = null;

   protected List<File> files = null;

   static
   {
      try
      {
         FileHandler fh = new FileHandler("target/fstest.log");
         fh.setFormatter(new SimpleFormatter());
         fh.setEncoding(System.getProperty("file.encoding"));
         // Send logger output to our FileHandler.
         log.addHandler(fh);
         // Request that every detail gets logged.
         log.setLevel(Level.ALL);
      }
      catch (Throwable e)
      {
         e.printStackTrace();
      }
   }

   protected class NameFilter implements FilenameFilter
   {

      private final String name;

      private final File dir;

      protected String getName()
      {
         return name;
      }

      protected File getDir()
      {
         return dir;
      }

      protected NameFilter(File dir, String name)
      {
         this.name = name;
         this.dir = dir;
      }

      protected NameFilter(String name)
      {
         this(null, name);
      }

      public boolean accept(File dir, String name)
      {
         return (this.dir != null ? this.dir.equals(dir) : true) && this.name.equals(name);
      }

   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      testRoot = new File("target/fstest");
      testRoot.mkdirs();
      PrivilegedFileHelper.deleteOnExit(testRoot);
   }

   @Override
   protected void tearDown() throws Exception
   {
      long time = System.currentTimeMillis();

      if (testRoot.exists())
      {
         deleteDir(testRoot);
         // deleteFiles(files);
         testRoot.delete();
         log.info("Tear down of " + getName() + ",\t" + (System.currentTimeMillis() - time));
      }

      super.tearDown();
   }

   protected void deleteFiles(List<File> filesList)
   {
      for (File f : filesList)
      {
         f.delete();
         deleteFileParent(new File(f.getParent()));
      }
   }

   protected void deleteFileParent(File fp)
   {
      if (fp.getAbsolutePath().startsWith(testRoot.getAbsolutePath()))
         if (fp.isDirectory())
         {
            String[] ls = fp.list();
            if (ls.length <= 0)
            {
               // log.info("del " + fp.getAbsolutePath());
               fp.delete();
               deleteFileParent(new File(fp.getParent()));
            }
         }
         else
            fail("Dir can't be a file but found " + fp.getAbsolutePath());
   }

   protected void deleteDir(File dir)
   {
      String[] ls = dir.list();
      if (ls == null)
      {
         log.log(Level.WARNING, "Dir not found " + dir.getAbsolutePath());
         fail("Dir not found " + dir.getAbsolutePath());
      }
      for (String fn : ls)
      {
         File f = new File(dir.getAbsolutePath() + File.separator + fn);
         if (f.isDirectory())
         {
            deleteDir(f);
            f.delete();
         }
         else
            f.delete();
      }
   }

   protected List<File> createPlainCase()
   {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i < FILES_COUNT; i++)
      {
         File f = new File(testRoot.getAbsolutePath() + File.separator + SIDGenerator.generate());
         try
         {
            // f.createNewFile();
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(f);
            try
            {
               fos.write(("File content " + f.getAbsolutePath()).getBytes());
            }
            finally
            {
               fos.close();
               files.add(f);
            }
         }
         catch (IOException e)
         {
            log.log(Level.WARNING, "File can't be created " + f, e);
         }
      }
      return this.files = files;
   }

   protected List<File> createTreeXCase()
   {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String fileName = SIDGenerator.generate();
         File dir = new File(testRoot.getAbsolutePath() + buildPathX(fileName));
         dir.mkdirs();
         File f = new File(dir.getAbsolutePath() + File.separator + fileName);
         try
         {
            // f.createNewFile();
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(f);
            try
            {
               fos.write(("File content " + f.getAbsolutePath()).getBytes());
            }
            finally
            {
               fos.close();
               files.add(f);
            }
         }
         catch (IOException e)
         {
            log.log(Level.WARNING, "File can't be created " + f, e);
         }
      }
      return this.files = files;
   }

   protected String buildPathX(String fileName)
   {
      char[] chs = fileName.toCharArray();
      String path = "";
      for (char ch : chs)
      {
         path += File.separator + ch;
      }
      return path;
   }

   protected List<File> createTreeXXCase()
   {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String fileName = SIDGenerator.generate();
         File dir = new File(testRoot.getAbsolutePath() + buildPathXX(fileName));
         dir.mkdirs();
         File f = new File(dir.getAbsolutePath() + File.separator + fileName);
         try
         {
            // f.createNewFile();
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(f);
            try
            {
               fos.write(("File content " + f.getAbsolutePath()).getBytes());
            }
            finally
            {
               fos.close();
               files.add(f);
            }
         }
         catch (IOException e)
         {
            log.log(Level.WARNING, "File can't be created " + f, e);
         }
      }
      return this.files = files;
   }

   protected String buildPathXX(String fileName)
   {
      char[] chs = fileName.toCharArray();
      String path = "";
      boolean block = true;
      for (char ch : chs)
      {
         path += block ? File.separator + ch : ch;
         block = !block;
      }
      return path;
   }

   protected List<File> createTreePrefixXCase()
   {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String fileName = SIDGenerator.generate();
         String prefix = fileName.substring(0, 24); // time + addr hash prefix
         String rnd = fileName.substring(24); // random name
         File dir = new File(testRoot.getAbsolutePath() + File.separator + prefix + File.separator + buildPathX(rnd));
         dir.mkdirs();
         File f = new File(dir.getAbsolutePath() + File.separator + fileName);
         try
         {
            // f.createNewFile();
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(f);
            try
            {
               fos.write(("File content " + f.getAbsolutePath()).getBytes());
            }
            finally
            {
               fos.close();
               files.add(f);
            }
         }
         catch (IOException e)
         {
            log.log(Level.WARNING, "File can't be created " + f, e);
         }
      }
      return this.files = files;
   }

   protected String buildPathX8(String fileName)
   {
      char[] chs = fileName.toCharArray();
      String path = "";
      final int xLength = 8;
      for (int i = 0; i < xLength; i++)
      {
         path += File.separator + chs[i];
      }
      path += File.separator + fileName.substring(xLength);
      return path;
   }

   protected List<File> createTreeX8Case()
   {
      List<File> files = new ArrayList<File>();
      for (int i = 0; i < FILES_COUNT; i++)
      {
         String fileName = SIDGenerator.generate();
         File dir = new File(testRoot.getAbsolutePath() + File.separator + buildPathX8(fileName));
         dir.mkdirs();
         File f = new File(dir.getAbsolutePath() + File.separator + fileName);
         try
         {
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(f);
            try
            {
               fos.write(("qazws").getBytes());
            }
            finally
            {
               fos.close();
               files.add(f);
            }
         }
         catch (IOException e)
         {
            log.log(Level.WARNING, "File can't be created " + f, e);
         }
      }
      return this.files = files;
   }

   protected void readFiles(NameFilter filter)
   {
      readFiles(testRoot, filter);
   }

   protected void readFiles(File root, NameFilter filter)
   {
      // long time = System.currentTimeMillis();
      // log.info(">>> Step into directory " + root.getAbsolutePath() +
      // (filter != null ? ", searched file " + filter.getName() : ""));
      String[] ls = filter != null ? root.list(filter) : root.list();
      for (String file : ls)
      {
         File f = new File(testRoot.getAbsolutePath() + File.separator + file);
         if (f.isDirectory())
         {
            // dir
            readFiles(f, filter);
         }
         else
         {
            // file
            try
            {
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(f);
               fis.close();
            }
            catch (FileNotFoundException e)
            {
               log.log(Level.WARNING, "File not found " + file, e);
            }
            catch (IOException e)
            {
               log.log(Level.WARNING, "File IO error " + file, e);
            }
         }
      }
      // log.info("<<< Exit from directory " + root.getAbsolutePath() + ",\t" +
      // (System.currentTimeMillis() - time));
   }

   protected void readTreeXFiles(NameFilter filter)
   {
      readTreeXFiles(testRoot, filter);
   }

   protected void readTreeXFiles(File root, NameFilter filter)
   {
      String dirPath = root.getAbsolutePath() + buildPathX(filter.getName());
      File dir = new File(dirPath);
      String[] ls = filter != null ? dir.list(filter) : dir.list();
      if (ls == null)
      {
         log.log(Level.WARNING, "Dir not found " + dir.getAbsolutePath());
         fail("Dir not found " + dir.getAbsolutePath());
      }

      for (String file : ls)
      {
         File f = new File(dir.getAbsolutePath() + File.separator + file);
         if (f.isDirectory())
         {
            // dir
            fail("The file can't be a dir but found " + f.getAbsolutePath());
         }
         else
         {
            // file
            try
            {
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(f);
               fis.close();
            }
            catch (FileNotFoundException e)
            {
               log.log(Level.WARNING, "File not found " + file, e);
            }
            catch (IOException e)
            {
               log.log(Level.WARNING, "File IO error " + file, e);
            }
         }
      }
   }

   protected void readTreeX8Files(File root, NameFilter filter)
   {
      String dirPath = root.getAbsolutePath() + buildPathX8(filter.getName());
      File dir = new File(dirPath);
      String[] ls = filter != null ? dir.list(filter) : dir.list();
      if (ls == null)
      {
         log.log(Level.WARNING, "Dir not found " + dir.getAbsolutePath());
         fail("Dir not found " + dir.getAbsolutePath());
      }

      for (String file : ls)
      {
         File f = new File(dir.getAbsolutePath() + File.separator + file);
         if (f.isDirectory())
         {
            // dir
            fail("The file can't be a dir but found " + f.getAbsolutePath());
         }
         else
         {
            // file
            try
            {
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(f);
               fis.close();
            }
            catch (FileNotFoundException e)
            {
               log.log(Level.WARNING, "File not found " + file, e);
            }
            catch (IOException e)
            {
               log.log(Level.WARNING, "File IO error " + file, e);
            }
         }
      }
   }

   protected void readTreeXXFiles(NameFilter filter)
   {
      readTreeXXFiles(testRoot, filter);
   }

   protected void readTreeXXFiles(File root, NameFilter filter)
   {
      String dirPath = root.getAbsolutePath() + buildPathXX(filter.getName());
      File dir = new File(dirPath);
      String[] ls = filter != null ? dir.list(filter) : dir.list();
      if (ls == null)
      {
         log.log(Level.WARNING, "Dir not found " + dir.getAbsolutePath());
         fail("Dir not found " + dir.getAbsolutePath());
      }

      for (String file : ls)
      {
         File f = new File(dir.getAbsolutePath() + File.separator + file);
         if (f.isDirectory())
         {
            // dir
            fail("The file can't be a dir but found " + f.getAbsolutePath());
         }
         else
         {
            // file
            try
            {
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(f);
               fis.close();
            }
            catch (FileNotFoundException e)
            {
               log.log(Level.WARNING, "File not found " + file, e);
            }
            catch (IOException e)
            {
               log.log(Level.WARNING, "File IO error " + file, e);
            }
         }
      }
   }

   protected void readTreePrefixXFiles(NameFilter filter)
   {
      readTreeFrefixXFiles(testRoot, filter);
   }

   protected void readTreeFrefixXFiles(File root, NameFilter filter)
   {
      String fileName = filter.getName();
      String prefix = fileName.substring(0, 24); // time + addr hash prefix
      String rnd = fileName.substring(24); // random name
      File dir = new File(testRoot.getAbsolutePath() + File.separator + prefix + File.separator + buildPathX(rnd));
      String[] ls = filter != null ? dir.list(filter) : dir.list();
      if (ls == null)
      {
         log.log(Level.WARNING, "Dir not found " + dir.getAbsolutePath());
         fail("Dir not found " + dir.getAbsolutePath());
      }

      for (String file : ls)
      {
         File f = new File(dir.getAbsolutePath() + File.separator + file);
         if (f.isDirectory())
         {
            // dir
            fail("The file can't be a dir but found " + f.getAbsolutePath());
         }
         else
         {
            // file
            try
            {
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(f);
               fis.close();
            }
            catch (FileNotFoundException e)
            {
               log.log(Level.WARNING, "File not found " + file, e);
            }
            catch (IOException e)
            {
               log.log(Level.WARNING, "File IO error " + file, e);
            }
         }
      }
   }

   public void _testPlainReadAll()
   {
      createPlainCase();

      long time = System.currentTimeMillis();
      readFiles(null);
      log.info(getName() + " -- " + (System.currentTimeMillis() - time));
   }

   public void _testPlainReadName()
   {
      List<File> files = createPlainCase();

      // readFiles(new NameFilter(files.get(FILES_COUNT - (FILES_COUNT>>1)).getAbsolutePath()));

      long time = System.currentTimeMillis();
      for (File f : files)
      {
         readFiles(new NameFilter(f.getName()));
      }
      log.info(getName() + " -- " + (System.currentTimeMillis() - time));
   }

   public void _testHierarchyTreeXReadName()
   {
      List<File> files = createTreeXCase();

      long time = System.currentTimeMillis();
      for (File f : files)
      {
         readTreeXFiles(new NameFilter(f.getName()));
      }
      log.info(getName() + " -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      deleteFiles(files);
      log.info("Delete -- " + (System.currentTimeMillis() - time));
   }

   public void _testHierarchyTreeXXReadName()
   {
      List<File> files = createTreeXXCase();

      long time = System.currentTimeMillis();
      for (File f : files)
      {
         readTreeXXFiles(new NameFilter(f.getName()));
      }
      log.info(getName() + " -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      deleteFiles(files);
      log.info("Delete -- " + (System.currentTimeMillis() - time));
   }

   public void _testHierarchyTreePrefixXReadName()
   {
      List<File> files = createTreePrefixXCase();

      long time = System.currentTimeMillis();
      for (File f : files)
      {
         readTreePrefixXFiles(new NameFilter(f.getName()));
      }
      log.info(getName() + " -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      deleteFiles(files);
      log.info("Delete -- " + (System.currentTimeMillis() - time));
   }

   public void testHierarchyTreeX8ReadName()
   {
      long time = System.currentTimeMillis();
      List<File> files = createTreeX8Case();
      log.info(getName() + " ADD -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      for (File f : files)
      {
         readTreeX8Files(testRoot, new NameFilter(f.getName()));
      }
      log.info(getName() + " READ -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      deleteFiles(files);
      log.info(getName() + " DELETE -- " + (System.currentTimeMillis() - time));
   }

}
