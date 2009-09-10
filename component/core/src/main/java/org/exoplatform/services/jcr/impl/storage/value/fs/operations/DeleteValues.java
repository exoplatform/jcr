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
package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: DeleteValues.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class DeleteValues extends ValueFileOperation
{

   /**
    * Files to be deleted.
    */
   private final File[] files;

   /**
    * Locks on files.
    */
   private ValueFileLock[] locks;

   /**
    * DeleteValues constructor.
    * 
    * @param files
    *          Files to be deleted
    * @param resources
    *          ValueDataResourceHolder
    * @param cleaner
    *          FileCleaner
    */
   public DeleteValues(File[] files, ValueDataResourceHolder resources, FileCleaner cleaner, File tempDir)
   {

      super(resources, cleaner, tempDir);

      this.files = files;
   }

   /**
    * {@inheritDoc}
    */
   public void execute() throws IOException
   {
      makePerformed();

      locks = new ValueFileLock[files.length];
      for (int i = 0; i < files.length; i++)
      {
         ValueFileLock fl = new ValueFileLock(files[i]);
         fl.lock();
         locks[i] = fl;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws IOException
   {
      if (locks != null)
         for (ValueFileLock fl : locks)
            fl.unlock();
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws IOException
   {
      if (locks != null)
         try
         {
            for (File f : files)
            {
               if (!f.delete())
                  // TODO possible place of error: FileNotFoundException when we delete/update existing
                  // Value and then add/update again.
                  // After the time the Cleaner will delete the file which is mapped to the Value.
                  // Don't use cleaner! Care about transaction-style files isolation per-user etc. 
                  cleaner.addFile(f);
            }
         }
         finally
         {
            if (locks != null)
               for (ValueFileLock fl : locks)
                  fl.unlock();
         }
   }
}
