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

   /**
    * Rename file.
    * 
    * @param srcFile
    *          source file
    * @param dstFile
    *          destination file 
    * @throws IOException
    *          if any exception occurred 
    */
   public static synchronized void renameFile(File srcFile, File dstFile) throws IOException
   {
      /* This is not atomic.  If the program crashes between the call to
         delete() and the call to renameTo() then we're screwed, but I've
         been unable to figure out how else to do this... */

      if (PrivilegedFileHelper.exists(dstFile))
         if (!PrivilegedFileHelper.delete(dstFile))
            throw new IOException("Cannot delete " + dstFile);

      // Rename the srcFile file to the new one. Unfortunately, the renameTo()
      // method does not work reliably under some JVMs.  Therefore, if the
      // rename fails, we manually rename by copying the srcFile file to the new one
      if (!PrivilegedFileHelper.renameTo(srcFile, dstFile))
      {
         java.io.InputStream in = null;
         java.io.OutputStream out = null;
         byte buffer[] = null;
         try
         {
            in = PrivilegedFileHelper.fileInputStream(srcFile);
            out = PrivilegedFileHelper.fileOutputStream(dstFile);
            // see if the buffer needs to be initialized. Initialization is
            // only done on-demand since many VM's will never run into the renameTo
            // bug and hence shouldn't waste 1K of mem for no reason.
            if (buffer == null)
            {
               buffer = new byte[1024];
            }
            int len;
            while ((len = in.read(buffer)) >= 0)
            {
               out.write(buffer, 0, len);
            }

            // delete the srcFile file.
            PrivilegedFileHelper.delete(srcFile);
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
      }
   }
}
