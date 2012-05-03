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

import org.exoplatform.services.jcr.impl.proccess.WorkerThread;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileCleaner.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class FileCleaner extends WorkerThread
{

   protected static final long DEFAULT_TIMEOUT = 30000;

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.FileCleaner");

   protected final ConcurrentLinkedQueue<File> files = new ConcurrentLinkedQueue<File>();

   public FileCleaner()
   {
      this(DEFAULT_TIMEOUT);
   }

   public FileCleaner(long timeout)
   {
      this(timeout, true);
   }

   public FileCleaner(boolean start)
   {
      this(DEFAULT_TIMEOUT, start);
   }

   public FileCleaner(long timeout, boolean start)
   {
      super(timeout);
      setName("FileCleaner " + getId());
      setDaemon(true);
      setPriority(Thread.MIN_PRIORITY);

      if (start)
         start();

      registerShutdownHook();
      if (log.isDebugEnabled())
      {
         log.debug("FileCleaner instantiated name= " + getName() + " timeout= " + timeout);
      }
   }

   /**
    * @param file
    */
   public void addFile(File file)
   {
      if (file.exists())
      {
         files.offer(file);
      }
   }

   /**
    * @param file
    */
   public void removeFile(File file)
   {
      files.remove(file);
   }
   
   public void halt()
   {
      try
      {
         callPeriodically();
      }
      catch (Exception e)
      {
      }

      if (files != null && files.size() > 0)
         log.warn("There are uncleared files: " + files.size());

      super.halt();
   }

   /**
    * @see org.exoplatform.services.jcr.impl.proccess.WorkerThread#callPeriodically()
    */
   protected void callPeriodically() throws Exception
   {
      File file = null;
      Set<File> notRemovedFiles = new HashSet<File>();
      while ((file = files.poll()) != null)
      {
         if (file.exists())
         {
            if (!file.delete())
            {
               notRemovedFiles.add(file);

               if (log.isDebugEnabled())
                  log.debug("Could not delete " + (file.isDirectory() ? "directory" : "file")
                     + ". Will try next time: " + file.getAbsolutePath());
            }
            else if (log.isDebugEnabled())
            {
               log.debug((file.isDirectory() ? "Directory" : "File") + " deleted : " + file.getAbsolutePath());
            }
         }
      }

      //add do lists tail all not removed files
      if (!notRemovedFiles.isEmpty())
      {
         files.addAll(notRemovedFiles);
      }
   }

   private void registerShutdownHook()
   {
      // register shutdown hook for final cleaning up
      try
      {
         Runtime.getRuntime().addShutdownHook(new Thread()
         {
            public void run()
            {
               File file = null;
               while ((file = files.poll()) != null)
               {
                  file.delete();
               }
            }
         });
      }
      catch (IllegalStateException e)
      {
         // can't register shutdownhook because
         // jvm shutdown sequence has already begun,
         // silently ignore...
      }
   }
}
