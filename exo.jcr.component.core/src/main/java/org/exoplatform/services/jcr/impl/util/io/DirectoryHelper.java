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

import org.exoplatform.commons.utils.PrivilegedFileHelper;

import java.io.File;
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

      if (!PrivilegedFileHelper.isDirectory(srcPath))
      {
         throw new IOException(PrivilegedFileHelper.getAbsolutePath(srcPath) + " is a directory");
      }

      for (File subFile : PrivilegedFileHelper.listFiles(srcPath))
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
      if (PrivilegedFileHelper.isDirectory(srcPath))
      {
         if (!PrivilegedFileHelper.exists(dstPath))
         {
            PrivilegedFileHelper.mkdirs(dstPath);
         }

         String files[] = PrivilegedFileHelper.list(srcPath);
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
            in = PrivilegedFileHelper.fileInputStream(srcPath);
            out = PrivilegedFileHelper.fileOutputStream(dstPath);

            // Transfer bytes from in to out
            byte[] buf = new byte[2048];

            int len;

            while ((len = in.read(buf)) > 0)
            {
               out.write(buf, 0, len);
            }
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
      if (PrivilegedFileHelper.isDirectory(dir))
      {
         for (File subFile : PrivilegedFileHelper.listFiles(dir))
         {
            removeDirectory(subFile);
         }

         if (!PrivilegedFileHelper.delete(dir))
         {
            throw new IOException("Can't remove folder : " + PrivilegedFileHelper.getCanonicalPath(dir));
         }
      }
      else
      {
         if (!PrivilegedFileHelper.delete(dir))
         {
            throw new IOException("Can't remove file : " + PrivilegedFileHelper.getCanonicalPath(dir));
         }
      }
   }

   /**
    * Compress directory.
    * 
    * @param srcPath
    *          source path
    * @param dstPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   public static void compressDirectory(File srcPath, File dstPath) throws IOException
   {
      if (PrivilegedFileHelper.isDirectory(srcPath))
      {
         if (!PrivilegedFileHelper.exists(dstPath))
         {
            PrivilegedFileHelper.mkdirs(dstPath);
         }

         String files[] = PrivilegedFileHelper.list(srcPath);
         for (int i = 0; i < files.length; i++)
         {
            compressDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         InputStream in = null;
         ZipOutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.fileInputStream(srcPath);
            out = PrivilegedFileHelper.zipOutputStream(dstPath);
            out.putNextEntry(new ZipEntry(srcPath.getName()));

            // Transfer bytes from in to out
            byte[] buf = new byte[2048];

            int len;

            while ((len = in.read(buf)) > 0)
            {
               out.write(buf, 0, len);
            }
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
               out.closeEntry();
               out.close();
            }
         }
      }
   }

   /**
    * Uncompress directory.
    * 
    * @param srcPath
    *          source path
    * @param dstPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   public static void uncompressDirectory(File srcPath, File dstPath) throws IOException
   {
      if (PrivilegedFileHelper.isDirectory(srcPath))
      {
         if (!PrivilegedFileHelper.exists(dstPath))
         {
            PrivilegedFileHelper.mkdirs(dstPath);
         }

         String files[] = PrivilegedFileHelper.list(srcPath);
         for (int i = 0; i < files.length; i++)
         {
            uncompressDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         ZipInputStream in = null;
         OutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.zipInputStream(srcPath);
            in.getNextEntry();
            out = PrivilegedFileHelper.fileOutputStream(dstPath);

            // Transfer bytes from in to out
            byte[] buf = new byte[2048];

            int len;

            while ((len = in.read(buf)) > 0)
            {
               out.write(buf, 0, len);
            }
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
}
