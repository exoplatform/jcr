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
import org.exoplatform.commons.utils.SecurityHelper;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 05.10.2007
 * 
 * For use in persistent layer, i.e. JDBCStorageConnection (ClenableFileValueData), will be shared
 * across the Workspace cache with many Sessions . Swap files creation (like in
 * JDBCStorageConnection.readValueData(String, int, int)) managed by SwapFile.get(File, String)
 * method. There are no way to get swap file in another way.
 * 
 * A SwapFile extends the SpoolFile. But in runtime all swap files will be stored in global map used
 * for prevent files rewriting in concurrent environment (case of issue JCR-329). Till the SwapFile
 * spool operation in progress all other users who will attempt to get the file will wait the
 * operation completion (java.util.concurrent.CountDownLatch used).
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SwapFile.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class SwapFile extends SpoolFile
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = 4048760909657109754L;

   /**
    * In-share files database.
    */
   protected static final ConcurrentMap<String, WeakReference<SwapFile>> CURRENT_SWAP_FILES =
      new ConcurrentHashMap<String, WeakReference<SwapFile>>();

   /**
    * Spool latch.
    */
   protected final AtomicReference<CountDownLatch> spoolLatch = new AtomicReference<CountDownLatch>();

   /**
    * SwapFile constructor.
    * 
    * @param parent
    *          Parent File
    * @param child
    *          File name
    */
   protected SwapFile(File parent, String child)
   {
      super(parent, child);
   }

   /**
    * Obtain SwapFile by parent file and name.
    * 
    * If the file was swapped before and still in use it will be returned, i.e. same object of
    * java.io.File will be returned.
    * 
    * If the file swapping (writing) now at this time the caller thread will wait till the swap
    * process will be finished.
    * 
    * @param parent
    *          - parent File
    * @param child
    *          - String with file name
    * @return SwapFile swap file
    * @throws IOException
    *           I/O error
    */
   public static SwapFile get(final File parent, final String child) throws IOException
   {
      SwapFile newsf = new SwapFile(parent, child);
      String absPath = PrivilegedFileHelper.getAbsolutePath(newsf);

      WeakReference<SwapFile> swappedRef = CURRENT_SWAP_FILES.get(absPath);
      SwapFile swapped;
      if (swappedRef != null && (swapped = swappedRef.get()) != null)
      {
         // The swap file has been registered already
         do
         {
            // We loop until the spoolLatch is null
            CountDownLatch spoolLatch = swapped.spoolLatch.get();
            if (spoolLatch != null)
            {
               try
               {
                  spoolLatch.await(); // wait till the file will be done
               }
               catch (final InterruptedException e)
               {
                  // thinking that is ok, i.e. this thread is interrupted
                  throw new IOException("Swap file read error " + PrivilegedFileHelper.getAbsolutePath(swapped) + ". "
                     + e)
                  {
                     @Override
                     public Throwable getCause()
                     {
                        return e;
                     }
                  };
               }
            }
         }
         while (!swapped.spoolLatch.compareAndSet(null, new CountDownLatch(1)));
         return swapped;
      }
      else if (swappedRef != null)
      {
         // The SwapFile has been garbage collected so we remove it from the map
         CURRENT_SWAP_FILES.remove(absPath, swappedRef);
      }
      newsf.spoolLatch.set(new CountDownLatch(1));

      WeakReference<SwapFile> currentValue = CURRENT_SWAP_FILES.putIfAbsent(absPath, new WeakReference<SwapFile>(newsf));
      if (currentValue != null)
      {
         // the swap file has been put already so we need to loop
         return get(parent, child);
      }
      return newsf;
   }

   /**
    * Tell if the file already spooled - ready for use.
    * 
    * @return boolean flag
    */
   public boolean isSpooled()
   {
      return spoolLatch.get() == null;
   }

   /**
    * Mark the file ready for read.
    */
   public void spoolDone()
   {
      final CountDownLatch sl = this.spoolLatch.get();
      this.spoolLatch.set(null);
      sl.countDown();
   }

   // ------ java.io.File ------

   /**
    * Delete file if it was not used by any other thread.
    */
   @Override
   public boolean delete()
   {
      return delete(false);
   }

   /**
    * Not applicable. Call get(File, String) method instead.
    * 
    * @throws IOException
    *           I/O error
    */
   public static SwapFile createTempFile(String prefix, String suffix, File directory) throws IOException
   {
      throw new IOException("Not applicable. Call get(File, String) method instead");
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         delete(true);
      }
      finally
      {
         super.finalize();
      }
   }
   
   /**
    * Deletes the file, if force is set to true, the map of users will be cleared to ensure that the deletion process won't be aborted
    */
   private boolean delete(boolean force)
   {
      String path = PrivilegedFileHelper.getAbsolutePath(this);
      WeakReference<SwapFile> currentValue = CURRENT_SWAP_FILES.get(path);
      if (currentValue == null || (currentValue.get() == this || currentValue.get() == null))
      {
         CURRENT_SWAP_FILES.remove(path, currentValue);            
         synchronized(this)
         {
            users.clear();
            final SpoolFile sf = this;

            PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>()
            {
               public Boolean run()
               {
                  if (sf.exists())
                  {
                     return SwapFile.super.delete();
                  }
                  return true;
               }
            };
            return SecurityHelper.doPrivilegedAction(action);
         }
      }
      return false;
   }
}