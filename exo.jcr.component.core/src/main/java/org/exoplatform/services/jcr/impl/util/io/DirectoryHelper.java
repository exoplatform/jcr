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
package org.exoplatform.services.jcr.impl.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by The eXo Platform SAS.
 *
 * Helper contains method to perform operations with not empty directory.
 *
 * Date: 25 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: FSDirectory.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DirectoryHelper
{

   /**
    * Returns the files list of whole directory including its sub directories. 
    *  
    * @param srcPath 
    *          source path 
    * @return List          
    * @throws IOException 
    *          if any exception occurred 
    */
   public static List<File> listFiles(File srcPath) throws IOException
   {
      List<File> result = new ArrayList<File>();

      if (!srcPath.isDirectory())
      {
         throw new IOException(srcPath.getAbsolutePath() + " is a directory");
      }

      for (File subFile : srcPath.listFiles())
      {
         result.add(subFile);
         if (subFile.isDirectory())
         {
            result.addAll(listFiles(subFile));
         }
      }

      return result;
   }

   /**
    * Copy directory.
    * 
    * @param srcPath
    *          source path
    * @param dstPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   public static void copyDirectory(File srcPath, File dstPath) throws IOException
   {
      if (srcPath.isDirectory())
      {
         if (!dstPath.exists())
         {
            dstPath.mkdirs();
         }

         String files[] = srcPath.list();
         for (int i = 0; i < files.length; i++)
         {
            copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         InputStream in = null;
         OutputStream out = null;

         try
         {
            in = new FileInputStream(srcPath);
            out = new FileOutputStream(dstPath);

            transfer(in, out);
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }

            if (out != null)
            {
               out.flush();
               out.close();
            }
         }
      }
   }

   /**
    * Remove directory.
    * 
    * @param dir
    *          directory to remove
    * @throws IOException
    *          if any exception occurred
    */
   public static void removeDirectory(File dir) throws IOException
   {
      if (dir.isDirectory())
      {
         for (File subFile : dir.listFiles())
         {
            removeDirectory(subFile);
         }

         if (!dir.delete())
         {
            throw new IOException("Can't remove folder : " + dir.getCanonicalPath());
         }
      }
      else
      {
         if (!dir.delete())
         {
            throw new IOException("Can't remove file : " + dir.getCanonicalPath());
         }
      }
   }

   /**
    * Compress data. In case when <code>rootPath</code> is a directory this method
    * compress all files and folders inside the directory into single one. IOException
    * will be thrown if directory is empty. If the <code>rootPath</code> is the file 
    * the only this file will be compressed. 
    * 
    * @param rootPath
    *          the root path, can be the directory or the file
    * @param dstZipPath
    *          the path to the destination compressed file
    * @throws IOException
    *          if any exception occurred
    */
   public static void compressDirectory(File rootPath, File dstZipPath) throws IOException
   {
      ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(dstZipPath));
      try
      {
         if (rootPath.isDirectory())
         {
            String files[] = rootPath.list();
            for (int i = 0; i < files.length; i++)
            {
               compressDirectory("", new File(rootPath, files[i]), zip);
            }
         }
         else
         {
            compressDirectory("", rootPath, zip);
         }
      }
      finally
      {
         if (zip != null)
         {
            zip.flush();
            zip.close();
         }
      }
   }

   /**
    * Compress files and directories. 
    */
   private static void compressDirectory(String relativePath, File srcPath, ZipOutputStream zip) throws IOException
   {
      if (srcPath.isDirectory())
      {
         zip.putNextEntry(new ZipEntry(relativePath + "/" + srcPath.getName() + "/"));
         zip.closeEntry();

         String files[] = srcPath.list();
         for (int i = 0; i < files.length; i++)
         {
            compressDirectory(relativePath + "/" + srcPath.getName(), new File(srcPath, files[i]), zip);
         }
      }
      else
      {
         InputStream in = new FileInputStream(srcPath);
         try
         {
            zip.putNextEntry(new ZipEntry(relativePath + "/" + srcPath.getName()));

            transfer(in, zip);

            zip.closeEntry();
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }
   }

   /**
    * Uncompress data to the destination directory. 
    * 
    * @param srcZipPath
    *          path to the compressed file, could be the file or the directory
    * @param dstDirPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   public static void uncompressDirectory(File srcZipPath, File dstDirPath) throws IOException
   {
      ZipInputStream in = new ZipInputStream(new FileInputStream(srcZipPath));
      ZipEntry entry = null;

      try
      {
         while ((entry = in.getNextEntry()) != null)
         {
            File dstFile = new File(dstDirPath, entry.getName());
            dstFile.getParentFile().mkdirs();

            if (entry.isDirectory())
            {
               dstFile.mkdirs();
            }
            else
            {
               OutputStream out = new FileOutputStream(dstFile);
               try
               {
                  transfer(in, out);
               }
               finally
               {
                  out.close();
               }
            }
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }
   }

   /**
    * Uncompress data to the destination directory. 
    * 
    * @param srcZipPath
    *          path to the compressed file, could be the file or the directory
    * @param dstDirPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   public static void uncompressEveryFileFromDirectory(File srcPath, File dstPath) throws IOException
   {
      if (srcPath.isDirectory())
      {
         if (!dstPath.exists())
         {
            dstPath.mkdirs();
         }

         String files[] = srcPath.list();
         for (int i = 0; i < files.length; i++)
         {
            uncompressEveryFileFromDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         ZipInputStream in = null;
         OutputStream out = null;

         try
         {
            in = new ZipInputStream(new FileInputStream(srcPath));
            in.getNextEntry();

            out = new FileOutputStream(dstPath);

            transfer(in, out);
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }

            if (out != null)
            {
               out.close();
            }
         }
      }
   }

   /**
    * Transfer bytes from in to out
    */
   public static void transfer(InputStream in, OutputStream out) throws IOException
   {
      byte[] buf = new byte[2048];

      int len;

      while ((len = in.read(buf)) > 0)
      {
         out.write(buf, 0, len);
      }
   }

   /**
    * Rename file. If file can't be renamed in standard way the coping
    * data will be used instead.
    * 
    * @param srcFile
    *          source file
    * @param dstFile
    *          destination file 
    * @throws IOException
    *          if any exception occurred 
    */
   public static void renameFile(File srcFile, File dstFile) throws IOException
   {
      // Rename the srcFile file to the new one. Unfortunately, the renameTo()
      // method does not work reliably under some JVMs.  Therefore, if the
      // rename fails, we manually rename by copying the srcFile file to the new one
      if (!srcFile.renameTo(dstFile))
      {
         InputStream in = null;
         OutputStream out = null;
         try
         {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(dstFile);

            transfer(in, out);
         }
         catch (IOException ioe)
         {
            IOException newExc = new IOException("Cannot rename " + srcFile + " to " + dstFile);
            newExc.initCause(ioe);
            throw newExc;
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }

            if (out != null)
            {
               out.close();
            }
         }

         // delete the srcFile file.
         srcFile.delete();
      }
   }
}
