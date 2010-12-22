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
package org.exoplatform.services.jcr.ext.script.groovy;

import groovy.lang.GroovyClassLoader;

import org.exoplatform.services.rest.ext.groovy.ClassPathEntry;
import org.exoplatform.services.rest.ext.groovy.GroovyClassLoaderProvider;
import org.exoplatform.services.rest.ext.groovy.ClassPathEntry.EntryType;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JcrGroovyClassLoaderProvider extends GroovyClassLoaderProvider
{
   /**
    * @see org.exoplatform.services.rest.ext.groovy.GroovyClassLoaderProvider#getGroovyClassLoader(org.exoplatform.services.rest.ext.groovy.ClassPathEntry[])
    */
   @Override
   public GroovyClassLoader getGroovyClassLoader(ClassPathEntry[] classPath) throws MalformedURLException
   {
      List<URL> files = new ArrayList<URL>();
      List<URL> roots = new ArrayList<URL>();
      for (int i = 0; i < classPath.length; i++)
      {
         ClassPathEntry classPathEntry = classPath[i];
         if (EntryType.SRC_DIR == classPathEntry.getType())
         {
            roots.add(classPathEntry.getPath());
         }
         else
         {
            files.add(classPathEntry.getPath());
         }
      }
      final GroovyClassLoader parent = getGroovyClassLoader();
      GroovyClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
         public GroovyClassLoader run()
         {
            return new GroovyClassLoader(parent);
         }
      });
      classLoader.setResourceLoader(new JcrGroovyResourceLoader(roots.toArray(new URL[roots.size()]), files
         .toArray(new URL[files.size()])));
      return classLoader;
   }
}