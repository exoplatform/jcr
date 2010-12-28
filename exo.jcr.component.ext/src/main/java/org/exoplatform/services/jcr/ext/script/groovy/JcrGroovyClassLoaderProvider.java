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

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.io.URLReaderSource;
import org.exoplatform.services.rest.ext.groovy.ExtendedGroovyClassLoader;
import org.exoplatform.services.rest.ext.groovy.GroovyClassLoaderProvider;
import org.exoplatform.services.rest.ext.groovy.SourceFolder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: JcrGroovyClassLoaderProvider.java 3702 2010-12-22 10:24:13Z
 *          aparfonov $
 */
public class JcrGroovyClassLoaderProvider extends GroovyClassLoaderProvider
{

   public static class JcrGroovyClassLoader extends ExtendedGroovyClassLoader
   {
      public JcrGroovyClassLoader(ClassLoader classLoader)
      {
         super(classLoader);
      }

      public JcrGroovyClassLoader(GroovyClassLoader parent)
      {
         super(parent);
      }

      /**
       * @see org.exoplatform.services.rest.ext.groovy.ExtendedGroovyClassLoader#createCompilationUnit(org.codehaus.groovy.control.CompilerConfiguration,
       *      java.security.CodeSource)
       */
      protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource cs)
      {
         return new JcrCompilationUnit(config, cs, this);
      }
   }

   public static class JcrCompilationUnit extends CompilationUnit
   {

      public JcrCompilationUnit()
      {
         super();
      }

      public JcrCompilationUnit(CompilerConfiguration configuration, CodeSource security, GroovyClassLoader loader,
         GroovyClassLoader transformLoader)
      {
         super(configuration, security, loader, transformLoader);
      }

      public JcrCompilationUnit(CompilerConfiguration configuration, CodeSource security, GroovyClassLoader loader)
      {
         super(configuration, security, loader);
      }

      public JcrCompilationUnit(CompilerConfiguration configuration)
      {
         super(configuration);
      }

      public JcrCompilationUnit(GroovyClassLoader loader)
      {
         super(loader);
      }

      /**
       * @see org.codehaus.groovy.control.CompilationUnit#addSource(java.net.URL)
       */
      @Override
      public SourceUnit addSource(URL url)
      {
         return addSource(new JcrSourceUnit(url, configuration, classLoader, getErrorCollector()));
      }
   }

   /** Adapter for JCR like URLs. */
   public static class JcrSourceUnit extends SourceUnit
   {
      public JcrSourceUnit(File source, CompilerConfiguration configuration, GroovyClassLoader loader, ErrorCollector er)
      {
         super(source, configuration, loader, er);
      }

      public JcrSourceUnit(String name, ReaderSource source, CompilerConfiguration flags, GroovyClassLoader loader,
         ErrorCollector er)
      {
         super(name, source, flags, loader, er);
      }

      public JcrSourceUnit(String name, String source, CompilerConfiguration configuration, GroovyClassLoader loader,
         ErrorCollector er)
      {
         super(name, source, configuration, loader, er);
      }

      public JcrSourceUnit(URL source, CompilerConfiguration configuration, GroovyClassLoader loader, ErrorCollector er)
      {
         /* jCR path is in fragment of URL:
          * jcr://repository/workspace#/path */
         super("jcr".equals(source.getProtocol()) ? source.getRef() : source.getPath(), new URLReaderSource(source,
            configuration), configuration, loader, er);
      }
   }

   public JcrGroovyClassLoaderProvider()
   {
      super(AccessController.doPrivileged(new PrivilegedAction<JcrGroovyClassLoader>() {
         public JcrGroovyClassLoader run()
         {
            return new JcrGroovyClassLoader(JcrGroovyClassLoaderProvider.class.getClassLoader());
         }
      }));
   }

   /**
    * @see org.exoplatform.services.rest.ext.groovy.GroovyClassLoaderProvider#getGroovyClassLoader(org.exoplatform.services.rest.ext.groovy.SourceFolder[])
    */
   @Override
   public ExtendedGroovyClassLoader getGroovyClassLoader(SourceFolder[] sources) throws MalformedURLException
   {
      if (sources == null || sources.length == 0)
         return getGroovyClassLoader();

      URL[] roots = new URL[sources.length];
      for (int i = 0; i < sources.length; i++)
         roots[i] = sources[i].getPath();

      final GroovyClassLoader parent = getGroovyClassLoader();
      JcrGroovyClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<JcrGroovyClassLoader>() {
         public JcrGroovyClassLoader run()
         {
            return new JcrGroovyClassLoader(parent);
         }
      });
      classLoader.setResourceLoader(new JcrGroovyResourceLoader(roots));
      return classLoader;
   }
}