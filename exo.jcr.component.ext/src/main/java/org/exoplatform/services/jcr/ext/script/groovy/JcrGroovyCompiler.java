/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.jcr.ext.script.groovy.JcrGroovyClassLoaderProvider.JcrGroovyClassLoader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ext.groovy.SourceFile;
import org.exoplatform.services.rest.ext.groovy.SourceFolder;
import org.picocontainer.Startable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JcrGroovyCompiler can load source code of groovy script from JCR and parse it
 * via GroovyClassLoader.
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JcrGroovyCompiler implements Startable
{
   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(JcrGroovyCompiler.class);

   protected final JcrGroovyClassLoaderProvider classLoaderProvider;

   protected List<GroovyScriptAddRepoPlugin> addRepoPlugins;

   public JcrGroovyCompiler(JcrGroovyClassLoaderProvider classLoaderProvider)
   {
      this.classLoaderProvider = classLoaderProvider;
   }

   public JcrGroovyCompiler()
   {
      this(new JcrGroovyClassLoaderProvider());
   }

   /**
    * Compile Groovy source that located in <code>sourceReferences</code>.
    * Compiled sources can be dependent to each other and dependent to Groovy
    * sources that are accessible for this compiler.
    * 
    * @param sourceReferences references to Groovy sources to be compiled
    * @return result of compilation
    * @throws IOException if any i/o errors occurs
    */
   public Class<?>[] compile(UnifiedNodeReference... sourceReferences) throws IOException
   {
      return compile(null, sourceReferences);
   }

   /**
    * Compile Groovy source that located in <code>sourceReferences</code>.
    * Compiled sources can be dependent to each other and dependent to Groovy
    * sources that are accessible for this compiler and with additional Groovy
    * sources <code>src</code>. <b>NOTE</b> To be able load Groovy source files
    * from specified folders the following rules must be observed:
    * <ul>
    * <li>Groovy source files must be located in folder with respect to package
    * structure</li>
    * <li>Name of Groovy source files must be the same as name of class located
    * in file</li>
    * <li>Groovy source file must have extension '.groovy'</li>
    * </ul>
    * <br>
    * Example: If source stream that we want compile contains the following
    * code:
    * 
    * <pre>
    *           package c.b.a
    *           
    *           import a.b.c.A
    *           
    *           class B extends A {
    *           // Do something.
    *           }
    * </pre>
    * 
    * Assume we store dependencies in JCR then URL of folder with Groovy sources
    * may be like this: <code>jcr://repository/workspace#/groovy-library</code>.
    * Then absolute path to JCR node that contains Groovy source must be as
    * following: <code>/groovy-library/a/b/c/A.groovy</code>
    * 
    * @param src additional Groovy source location that should be added in
    *           class-path when compile <code>sourceReferences</code>
    * @param sourceReferences references to Groovy sources to be compiled
    * @return result of compilation
    * @throws IOException if any i/o errors occurs
    */
   public Class<?>[] compile(SourceFolder[] src, UnifiedNodeReference... sourceReferences) throws IOException
   {
      SourceFile[] files = new SourceFile[sourceReferences.length];
      for (int i = 0; i < sourceReferences.length; i++)
         files[i] = new SourceFile(sourceReferences[i].getURL());
      return doCompile((JcrGroovyClassLoader)classLoaderProvider.getGroovyClassLoader(src), files);
   }

   /**
    * Compile Groovy source that located in <code>files</code>. Compiled sources
    * can be dependent to each other and dependent to Groovy sources that are
    * accessible for this compiler and with additional Groovy sources
    * <code>src</code>. <b>NOTE</b> To be able load Groovy source files from
    * specified folders the following rules must be observed:
    * <ul>
    * <li>Groovy source files must be located in folder with respect to package
    * structure</li>
    * <li>Name of Groovy source files must be the same as name of class located
    * in file</li>
    * <li>Groovy source file must have extension '.groovy'</li>
    * </ul>
    * 
    * @param src additional Groovy source location that should be added in
    *           class-path when compile <code>files</code>
    * @param files Groovy sources to be compiled
    * @return result of compilation
    * @throws IOException if any i/o errors occurs
    */
   public Class<?>[] compile(SourceFolder[] src, SourceFile[] files) throws IOException
   {
      return doCompile((JcrGroovyClassLoader)classLoaderProvider.getGroovyClassLoader(src), files);
   }

   /**
    * Compile Groovy source that located in <code>files</code>. Compiled sources
    * can be dependent to each other and dependent to Groovy sources that are
    * accessible for this compiler.
    * 
    * @param files Groovy sources to be compiled
    * @return result of compilation
    * @throws IOException if any i/o errors occurs
    */
   public Class<?>[] compile(SourceFile[] files) throws IOException
   {
      return doCompile((JcrGroovyClassLoader)classLoaderProvider.getGroovyClassLoader(), files);
   }

   @SuppressWarnings("rawtypes")
   private Class<?>[] doCompile(final JcrGroovyClassLoader cl, final SourceFile[] files) throws IOException
   {
      Class[] classes = SecurityHelper.doPrivilegedAction(new PrivilegedAction<Class[]>() {
         public Class[] run()
         {
            return cl.parseClasses(files);
         }
      });
      return classes;
   }

   /**
    * Get URLs of classes (stored in JCR only) required to compile sources
    * <code>files</code>. Result array includes URLs of <code>files</code> plus
    * URLs of other required sources if they can be found in class-path.
    * 
    * @param sources additional Groovy source location that should be added in
    *           class-path when analyze <code>files</code>
    * @param files set of sources for analyzing
    * @return URLs
    * @throws IOException if any i/o errors occurs
    */
   public URL[] getDependencies(final SourceFolder[] sources, final SourceFile[] files) throws IOException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<URL[]>() {
            public URL[] run() throws IOException
            {
               return ((JcrGroovyClassLoader)classLoaderProvider.getGroovyClassLoader()).findDependencies(sources,
                  files);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         throw (IOException)cause;
      }
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      if (addRepoPlugins != null && addRepoPlugins.size() > 0)
      {
         try
         {
            Set<URL> repos = new HashSet<URL>();
            for (GroovyScriptAddRepoPlugin pl : addRepoPlugins)
               repos.addAll(pl.getRepositories());
            classLoaderProvider.getGroovyClassLoader().setResourceLoader(
               new JcrGroovyResourceLoader(repos.toArray(new URL[repos.size()])));
         }
         catch (MalformedURLException e)
         {
            LOG.error("Unable add groovy script repository. ", e);
         }
      }
   }

   /**
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
   }
}
