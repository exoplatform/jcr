/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.ValueOperation;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileLockException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueFileOperation.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public abstract class ValueFileOperation extends ValueFileIOHelper implements ValueOperation
{

   /**
    * Temporary files extension.
    */
   public static final String TEMP_FILE_EXTENSION = ".temp";

   /**
    * Lock files extension.
    */
   public static final String LOCK_FILE_EXTENSION = ".lock";

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueFileOperation");

   /**
    * The local internet address
    */
   private static String LOCAL_ADDRESS;
   static
   {
      try
      {
         // get the inet address
         InetAddress local = InetAddress.getLocalHost();
         LOCAL_ADDRESS = local.getHostAddress() + " (" + local.getHostName() + ")";
      }
      catch (UnknownHostException e)
      {
         LOG.warn("Cannot read host address " + e.getMessage());
         LOCAL_ADDRESS = "no address, " + e;
      }      
   }

   /**
    * File cleaner.
    */
   protected final FileCleaner cleaner;

   /**
    * Resources resources.
    */
   protected final ValueDataResourceHolder resources;

   /**
    * Operation info (not used).
    */
   protected final String operationInfo;

   /**
    * Directory for temporary operations (temp files and locks).
    */
   protected final File tempDir;

   /**
    * Performed state flag. Optional for use.
    */
   private boolean performed = false;

   /**
    * Internal ValueLockSupport implementation.
    * 
    */
   class ValueFileLockHolder implements ValueLockSupport
   {

      /**
       * Target File.
       */
      private final File targetFile;

      /**
       * Lock File.
       */
      private File lockFile;

      /**
       * Lock File stream.
       */
      private FileOutputStream lockFileStream;

      /**
       * ValueFileLockHolder constructor.
       * 
       * @param file
       *          target File
       */
      ValueFileLockHolder(File file)
      {
         this.targetFile = file;
      }

      /**
       * {@inheritDoc}
       */
      public void lock() throws IOException
      {
         // lock file in temp directory
         lockFile = new File(tempDir, targetFile.getName() + LOCK_FILE_EXTENSION);

         FileOutputStream lout = new FileOutputStream(lockFile, true);
         lout.write(operationInfo.getBytes());
         lout.getChannel().lock(); // wait for unlock (on Windows will wait for this JVM too)

         lockFileStream = lout;
      }

      /**
       * {@inheritDoc}
       */
      public void share(ValueLockSupport anotherLock) throws IOException
      {
         if (anotherLock instanceof ValueFileLockHolder)
         {
            ValueFileLockHolder al = (ValueFileLockHolder)anotherLock;
            lockFile = al.lockFile;
            lockFileStream = al.lockFileStream;
         }
         else
            throw new IOException("Cannot share lock with " + anotherLock.getClass());
      }

      /**
       * {@inheritDoc}
       */
      public void unlock() throws IOException
      {
         if (lockFileStream != null)
            lockFileStream.close();

         if (!lockFile.delete())
         {
            LOG.warn("Cannot delete lock file " + lockFile.getAbsolutePath() + ". Add to the FileCleaner");
            cleaner.addFile(lockFile);
         }
      }

   }

   /**
    * Value File lock abstraction.
    * 
    */
   class ValueFileLock
   {

      /**
       * Target file.
       */
      private final File file;

      /**
       * ValueFileLock constructor.
       * 
       * @param file
       *          File
       */
      ValueFileLock(File file)
      {
         this.file = file;
      }

      /**
       * Lock File location (place on file-system) for this JVM and external changes.
       * 
       * @return boolean, true - if locked, false - if already locked by this Thread
       * @throws IOException
       *           if error occurs
       */
      public boolean lock() throws IOException
      {
         // lock in JVM (wait for unlock if required)
         try
         {
            return resources.aquire(file.getAbsolutePath(), new ValueFileLockHolder(file));
         }
         catch (InterruptedException e)
         {
            throw new FileLockException("Lock error on " + file.getAbsolutePath(), e);
         }
      }

      /**
       * Unlock File location (place on file-system) for this JVM and external changes.
       * 
       * @return boolean, true - if unlocked, false - if still locked by this Thread
       * @throws IOException
       *           if error occurs
       */
      public boolean unlock() throws IOException
      {
         return resources.release(file.getAbsolutePath());
      }
   }

   /**
    * ValueFileOperation constructor.
    * 
    * @param resources
    *          ValueDataResourceHolder
    * @param cleaner
    *          FileCleaner
    * @param tempDir
    *          temp dir for locking and other I/O operations
    */
   ValueFileOperation(ValueDataResourceHolder resources, FileCleaner cleaner, File tempDir)
   {

      this.cleaner = cleaner;
      this.resources = resources;

      this.tempDir = tempDir;

      operationInfo = System.currentTimeMillis() + " " + LOCAL_ADDRESS;
   }

   /**
    * Tell if operation was performed.
    * 
    * @return boolean - true if operation was performed
    */
   protected boolean isPerformed()
   {
      return performed;
   }

   /**
    * Make operation as performed.
    * 
    * @throws IOException
    *           if operation was already performed
    */
   protected void makePerformed() throws IOException
   {
      if (performed)
         throw new IOException("Operation cannot be performed twice");

      performed = true;
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws IOException
   {
      try
      {
         prepare();         
      }
      finally
      {
         twoPhaseCommit();         
      }
   }
   
   /**  
    * Moves a file from a location to another by using the method {{File.renameTo(File)}}  
    * if it fails it will do a copy and delete. If the source file cannot be deleted  
    * it will be given to the file cleaner  
    * @param srcFile the file to be moved  
    * @param destFile the destination file  
    * @throws IOException if error occurs  
    */
   protected void move(File srcFile, File destFile) throws IOException
   {
      if (!srcFile.renameTo(destFile))
      {
         try
         {
            copyClose(new FileInputStream(srcFile), new FileOutputStream(destFile));
         }
         catch (IOException e)
         {
            throw new IOException("Could not move the file from " + srcFile.getAbsolutePath() + " to "
               + destFile.getAbsolutePath(), e);
         }
         if (!srcFile.delete())
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug("The file '"
                  + srcFile.getAbsolutePath()  
                  + "' could not be deleted which prevents the application"  
                  + " to move it properly, it is probably due to a stream used to read this property "  
                  + "that has not been closed as expected. The file will be given to the file cleaner for a later deletion.");  
            }  
            // The source could not be deleted so we add it to the  
            // file cleaner  
            cleaner.addFile(srcFile);  
         }  
      }  
   }
}
