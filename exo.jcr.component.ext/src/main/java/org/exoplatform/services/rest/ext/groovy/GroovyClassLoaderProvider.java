/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.services.rest.ext.groovy;

import groovy.lang.GroovyClassLoader;


import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory of Groovy class loader. It can provide preset GroovyClassLoader
 * instance or customized instance of GroovyClassLoader able resolve additional
 * Groovy source files.
 * 
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: GroovyClassLoaderProvider.java 3701 2010-12-22 10:15:37Z
 *          aparfonov $
 */
public class GroovyClassLoaderProvider
{
   /** Preset default GroovyClassLoader. */
   private ExtendedGroovyClassLoader defaultClassLoader;

   public GroovyClassLoaderProvider()
   {
      this(new ExtendedGroovyClassLoader(GroovyClassLoaderProvider.class.getClassLoader()));
   }

   protected GroovyClassLoaderProvider(ExtendedGroovyClassLoader defaultClassLoader)
   {
      this.defaultClassLoader = defaultClassLoader;
   }

   /**
    * Get default GroovyClassLoader.
    * 
    * @return default GroovyClassLoader
    */
   public ExtendedGroovyClassLoader getGroovyClassLoader()
   {
      return defaultClassLoader;
   }

   /**
    * Get customized instance of GroovyClassLoader that able to resolve
    * additional Groovy source files.
    * 
    * @param sources additional Groovy sources
    * @return GroovyClassLoader
    * @throws MalformedURLException if any of entries in <code>sources</code>
    *            has invalid URL.
    */
   public ExtendedGroovyClassLoader getGroovyClassLoader(SourceFolder[] sources) throws MalformedURLException
   {
      if (sources == null || sources.length == 0)
         return getGroovyClassLoader();

      URL[] roots = new URL[sources.length];
      for (int i = 0; i < sources.length; i++)
         roots[i] = sources[i].getPath();

      final GroovyClassLoader parent = getGroovyClassLoader();
      ExtendedGroovyClassLoader classLoader = new ExtendedGroovyClassLoader(parent);
      classLoader.setResourceLoader(new DefaultGroovyResourceLoader(roots));
      return classLoader;
   }
}
