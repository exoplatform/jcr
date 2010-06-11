/*
 * Copyright (C) 2010 eXo Platform SAS.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: SecurityFileHelper.java 111 2010-11-11 11:11:11Z tolusha $
 *
 * Class helper need for perform privileged file operations.
 */
public class PrivilegedFileHelper
{

   public static FileOutputStream fileOutputStream(final File file) throws FileNotFoundException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            return new FileOutputStream(file);
         }
      };
      try
      {
         return (FileOutputStream)AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof FileNotFoundException)
         {
            throw (FileNotFoundException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   public static FileInputStream fileInputStream(final File file) throws FileNotFoundException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            return new FileInputStream(file);
         }
      };
      try
      {
         return (FileInputStream)AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof FileNotFoundException)
         {
            throw (FileNotFoundException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   public static long length(final File file)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return new Long(file.length());
         }
      };
      return (Long)AccessController.doPrivileged(action);
   }

   public static String getAbsolutePath(final File file)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return file.getAbsolutePath();
         }
      };
      return (String)AccessController.doPrivileged(action);
   }

   public static boolean delete(final File file)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return file.delete();
         }
      };
      return (Boolean)AccessController.doPrivileged(action);
   }

   public static boolean exists(final File file)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return file.exists();
         }
      };
      return (Boolean)AccessController.doPrivileged(action);
   }

   public static void mkdirs(final File file)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            file.mkdirs();
            return null;
         }
      };
      AccessController.doPrivileged(action);
   }

   public static boolean renameTo(final File srcFile, final File dstfile)
   {
      PrivilegedAction<Object> action = new PrivilegedAction<Object>()
      {
         public Object run()
         {
            return new Boolean(srcFile.renameTo(dstfile));
         }
      };
      return (Boolean)AccessController.doPrivileged(action);
   }
}
