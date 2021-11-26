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

package org.exoplatform.services.jcr.impl.util.io;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 05.10.2007
 * 
 * For use in TransienValueData (may be shared in the Workspace cache with another Sessions). Spool
 * files used in ValueData for incoming values managed by SpoolFile class. Spool file may be created
 * with constructor or be obtained from static method createTempFile(String, String, File), which
 * itself create physical file using PrivilegedFileHelper.createTempFile(prefix, suffix, directory) call. Spool file
 * may be acquired for usage by any object (SpoolFile.acquire(Object)). Till this object will call
 * release (SpoolFile.release(Object)) or will be garbage collected it's impossible to delete the
 * spool file (by File.delete() method).
 * 
 * Spool file will be created in TransientValueData during a source stream spool operation (caused
 * by getAsBytes() or getAsStream()). This file is a own of this ValueData, which itself contains in
 * Property and Session. After the save this file (as part of ValueData) will become a part of
 * workspace cache and may be shared with other sessions. After the JCR core restart (JVM restart)
 * ValueData will use file/BLOB from workspace storage.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SpoolFile.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class SpoolFile extends File
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SpoolFile");

   protected Map<Object, Long> users = new WeakHashMap<Object, Long>();

   public SpoolFile(File parent, String child)
   {
      super(parent, child);
   }

   public SpoolFile(String absPath)
   {
      super(absPath);
   }

   public static SpoolFile createTempFile(final String prefix, final String suffix, final File directory)
      throws IOException
   {
      return new SpoolFile(PrivilegedFileHelper.getAbsolutePath(PrivilegedFileHelper.createTempFile(prefix, suffix,
         directory)));
   }

   public synchronized void acquire(Object holder) throws FileNotFoundException
   {
      if (users == null)
         throw new FileNotFoundException("File was deleted " + PrivilegedFileHelper.getAbsolutePath(this));

      users.put(holder, System.currentTimeMillis());
   }

   public synchronized void release(Object holder) throws FileNotFoundException
   {
      if (users == null)
         throw new FileNotFoundException("File was deleted " + PrivilegedFileHelper.getAbsolutePath(this));

      users.remove(holder);
   }

   public synchronized boolean inUse() throws FileNotFoundException
   {
      if (users == null)
         throw new FileNotFoundException("File was deleted " + PrivilegedFileHelper.getAbsolutePath(this));

      return users.size() > 0;
   }

   // ------- java.io.File -------

   @Override
   public synchronized boolean delete()
   {
      if (users != null && users.size() <= 0)
      {
         // make unusable
         users.clear();
         users = null;

         final SpoolFile sf = this;

         PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>()
         {
            public Boolean run()
            {
               return sf.exists() ? SpoolFile.super.delete() : true;
            }
         };
         return SecurityHelper.doPrivilegedAction(action);

      }

      return false;
   }

}
