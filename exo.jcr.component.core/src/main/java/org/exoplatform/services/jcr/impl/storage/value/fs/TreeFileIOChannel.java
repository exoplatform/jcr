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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 22.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TreeFileIOChannel.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TreeFileIOChannel extends FileIOChannel
{

   private static final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>(64, 0.75f, 64);

   TreeFileIOChannel(File rootDir, FileCleaner cleaner, String storageId, ValueDataResourceHolder resources)
   {
      super(rootDir, cleaner, storageId, resources);
   }

   @Override
   protected String makeFilePath(final String propertyId, final int orderNumber)
   {
      return buildPath(propertyId) + File.separator + propertyId + orderNumber;
   }

   @Override
   protected File getFile(final String propertyId, final int orderNumber) throws IOException
   {
      final TreeFile tfile =
         new TreeFile(rootDir.getAbsolutePath() + makeFilePath(propertyId, orderNumber), cleaner, rootDir);
      mkdirs(tfile.getParentFile()); // make dirs on path
      return tfile;
   }

   @Override
   protected File[] getFiles(final String propertyId) throws IOException
   {
      final File dir = new File(rootDir.getAbsolutePath() + buildPath(propertyId));
      String[] fileNames = dir.list();
      File[] files = new File[fileNames.length];
      for (int i = 0; i < fileNames.length; i++)
      {
         files[i] = new TreeFile(dir.getAbsolutePath() + File.separator + fileNames[i], cleaner, rootDir);
      }
      return files;
   }

   protected String buildPath(String fileName)
   {
      return buildPathX8(fileName);
   }

   // not useful, as it slow in read/write
   protected String buildPathX(String fileName)
   {
      char[] chs = fileName.toCharArray();
      StringBuilder path = new StringBuilder();
      for (char ch : chs)
      {
         path.append(File.separator).append(ch);
      }
      return path.toString();
   }

   // best for now, 12.07.07
   protected String buildPathX8(String fileName)
   {
      final int xLength = 8;
      char[] chs = fileName.toCharArray();
      StringBuilder path = new StringBuilder();
      for (int i = 0; i < xLength; i++)
      {
         path.append(File.separator).append(chs[i]);
      }
      path.append(fileName.substring(xLength));
      return path.toString();
   }

   protected String buildPathXX2X4(String fileName)
   {
      final int xxLength = 4;
      final int xLength = 8;
      boolean xxBlock = true;
      char[] chs = fileName.toCharArray();
      StringBuilder path = new StringBuilder();

      for (int xxi = 0; xxi < xxLength; xxi++)
      {
         char ch = chs[xxi];
         path.append(xxBlock ? File.separator + ch : ch);
         xxBlock = !xxBlock;
      }
      for (int xi = xxLength; xi < xLength; xi++)
      {
         path.append(File.separator).append(chs[xi]);
      }
      path.append(fileName.substring(xLength));
      return path.toString();
   }

   protected String buildPathXX(String fileName)
   {
      char[] chs = fileName.toCharArray();
      StringBuilder path = new StringBuilder();
      boolean block = true;
      for (char ch : chs)
      {
         path.append(block ? File.separator + ch : ch);
         block = !block;
      }
      return path.toString();
   }

   protected String buildPathXX8(String fileName)
   {
      final int xxLength = 16; // length / 2 = xx length
      char[] chs = fileName.toCharArray();
      StringBuilder path = new StringBuilder();
      boolean block = true;
      for (int i = 0; i < xxLength; i++)
      {
         char ch = chs[i];
         path.append(block ? File.separator + ch : ch);
         block = !block;
      }
      path.append(fileName.substring(xxLength));
      return path.toString();
   }

   private static void mkdirs(File dir)
   {
      if (dir.exists())
      {
         return;
      }
      List<File> dir2Create = new ArrayList<File>();
      dir2Create.add(dir);
      dir = dir.getParentFile();
      while (dir != null && !dir.exists())
      {
         dir2Create.add(0, dir);
         dir = dir.getParentFile();
      }
      for (int i = 0, length = dir2Create.size(); i < length; i++)
      {
         mkdir(dir2Create.get(i));
      }
   }
   
   private static void mkdir(File dir)
   {
      String path = dir.getAbsolutePath();
      Lock lock = locks.get(path);
      if (lock == null)
      {
         lock = new ReentrantLock();
         Lock prevLock = locks.putIfAbsent(path, lock);
         if (prevLock != null)
         {
            lock = prevLock;
         }
      }
      lock.lock();
      try
      {
         if (!dir.exists())
         {
            dir.mkdir();
         }
      }
      finally
      {
         lock.unlock();
         locks.remove(path, lock);
      }
   }
}
