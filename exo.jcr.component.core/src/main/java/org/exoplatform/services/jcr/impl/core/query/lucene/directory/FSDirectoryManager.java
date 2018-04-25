/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene.directory;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * <code>FSDirectoryManager</code> implements a directory manager for
 * {@link FSDirectory} instances.
 */
@SuppressWarnings("unchecked")
public class FSDirectoryManager implements DirectoryManager
{

   private static Class<? extends FSDirectory> FS_DIRECTORY_CLASS;

   private static Class<? extends LockFactory> LOCK_FACTORY_CLASS;

   static
   {
      // get eXo system properties 
      String lockFactoryClassName =
         PropertyManager.getProperty("org.exoplatform.jcr.lucene.store.FSDirectoryLockFactoryClass");
      String fsDirectoryClassName = PropertyManager.getProperty("org.exoplatform.jcr.lucene.FSDirectory.class");
      // map to Lucene ones. Works only with Lucene 2.x.
      if (lockFactoryClassName != null)
      {
         try
         {
            // avoid case when abstract base class used
            if (!FSLockFactory.class.getName().equals(lockFactoryClassName))
            {
               LOCK_FACTORY_CLASS = (Class<? extends LockFactory>)Class.forName(lockFactoryClassName);
            }
         }
         catch (ClassNotFoundException e)
         {
            throw new RuntimeException("cannot load LockFactory class: " + e.toString(), e);
         }
      }

      if (fsDirectoryClassName != null)
      {
         try
         {
            // avoid case when abstract base class used
            if (!FSDirectory.class.getName().equals(fsDirectoryClassName))
            {
               FS_DIRECTORY_CLASS = (Class<? extends FSDirectory>)Class.forName(fsDirectoryClassName);
            }
            // else rely on Lucene FSDirectory instantiation 
         }
         catch (ClassNotFoundException e)
         {
            throw new RuntimeException("cannot load FSDirectory class: " + e.toString(), e);
         }
      }
   }

   /**
    * The base directory.
    */
   private File baseDir;

   /**
    * {@inheritDoc}
    */
   public void init(final SearchIndex handler) throws IOException
   {
      SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            baseDir = new File(handler.getPath());
            return null;
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public void init(final String path) throws IOException
   {
      SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            baseDir = new File(path);
            return null;
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasDirectory(final String name) throws IOException
   {
      return SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Boolean>()
      {
         public Boolean run() throws Exception
         {
            return new File(baseDir, name).exists();

         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public Directory getDirectory(final String name) throws IOException
   {
      return SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Directory>()
      {
         public Directory run() throws Exception
         {
            File dir;
            if (name.equals("."))
            {
               dir = baseDir;
            }
            else
            {
               dir = new File(baseDir, name);
            }
            // FSDirectory itself doesnt create dirs now
            if (!dir.exists())
            {
               if (!dir.mkdirs())
               {
                  throw new IOException("Cannot create directory: " + dir);
               }
            }
            LockFactory lockFactory =
               (LOCK_FACTORY_CLASS == null) ? new NativeFSLockFactory() : (LockFactory)LOCK_FACTORY_CLASS.newInstance();

            if (FS_DIRECTORY_CLASS == null)
            {
               return FSDirectory.open(dir, lockFactory);
            }
            else
            {
               Constructor<? extends FSDirectory> constructor =
                  FS_DIRECTORY_CLASS.getConstructor(File.class, LockFactory.class);
               return constructor.newInstance(dir, lockFactory);
            }
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public String[] getDirectoryNames() throws IOException
   {
      return SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<String[]>()
      {
         public String[] run() throws Exception
         {
            File[] dirs = baseDir.listFiles(new FileFilter()
            {
               public boolean accept(File pathname)
               {
                  return pathname.isDirectory();
               }
            });
            if (dirs != null)
            {
               String[] names = new String[dirs.length];
               for (int i = 0; i < dirs.length; i++)
               {
                  names[i] = dirs[i].getName();
               }
               return names;
            }
            else
            {
               throw new IOException("listFiles for " + baseDir.getPath() + " returned null");
            }
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public boolean delete(final String name)
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Boolean>()
      {
         public Boolean run()
         {
            File directory = new File(baseDir, name);
            // trivial if it does not exist anymore
            if (!directory.exists())
            {
               return true;
            }
            // delete files first
            File[] files = directory.listFiles();
            if (files != null)
            {
               for (int i = 0; i < files.length; i++)
               {
                  if (!files[i].delete())
                  {
                     return false;
                  }
               }
            }
            else
            {
               return false;
            }
            // now delete directory itself
            return directory.delete();
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   public boolean rename(final String from, final String to)
   {
      final File src = new File(baseDir, from);
      final File dest = new File(baseDir, to);

      try
      {
         SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws IOException
            {
               DirectoryHelper.renameFile(src, dest);

               return null;
            }
         });
      }
      catch (IOException e)
      {
         return false;
      }

      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void dispose()
   {
   }
}
