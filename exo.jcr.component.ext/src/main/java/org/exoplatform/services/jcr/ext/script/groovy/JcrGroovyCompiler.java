/**
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
import groovy.lang.GroovyCodeSource;

import org.codehaus.groovy.control.CompilationFailedException;
import org.exoplatform.services.jcr.ext.resource.JcrURLConnection;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ext.groovy.ClassPathEntry;
import org.exoplatform.services.rest.ext.groovy.ClassPathEntry.EntryType;
import org.exoplatform.services.rest.ext.groovy.GroovyClassLoaderProvider;
import org.picocontainer.Startable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
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

   protected final GroovyClassLoaderProvider classLoaderProvider;

   protected List<GroovyScriptAddRepoPlugin> addRepoPlugins;

   protected JcrGroovyCompiler(GroovyClassLoaderProvider classLoaderProvider)
   {
      this.classLoaderProvider = classLoaderProvider;
   }

   public JcrGroovyCompiler()
   {
      classLoaderProvider = new JcrGroovyClassLoaderProvider();
   }

   public Class<?>[] compile(UnifiedNodeReference... sourceReferences) throws IOException
   {
      // Add all compiled entries in class-path. Need to do this to resolve dependencies between compiled files.
      ClassPathEntry[] classPath = new ClassPathEntry[sourceReferences.length];
      for (int i = 0; i < classPath.length; i++)
         classPath[i] = new JcrClassPathEntry(EntryType.FILE, sourceReferences[i]);
      return doCompile(classLoaderProvider.getGroovyClassLoader(classPath), sourceReferences);
   }

   public Class<?>[] compile(ClassPathEntry[] classPath, UnifiedNodeReference... sourceReferences) throws IOException
   {
      ClassPathEntry[] compiled = new ClassPathEntry[sourceReferences.length];
      for (int i = 0; i < compiled.length; i++)
         compiled[i] = new JcrClassPathEntry(EntryType.FILE, sourceReferences[i]);
      ClassPathEntry[] fullClassPath = new ClassPathEntry[compiled.length + classPath.length];
      System.arraycopy(compiled, 0, fullClassPath, 0, compiled.length);
      System.arraycopy(classPath, 0, fullClassPath, compiled.length, classPath.length);
      return doCompile(classLoaderProvider.getGroovyClassLoader(fullClassPath), sourceReferences);
   }

   private Class<?>[] doCompile(final GroovyClassLoader cl, final UnifiedNodeReference... sourceReferences)
      throws IOException
   {
      Class<?>[] classes = new Class<?>[sourceReferences.length];
      for (int i = 0; i < sourceReferences.length; i++)
      {
         JcrURLConnection conn = null;
         try
         {
            final URL url = sourceReferences[i].getURL();
            conn = (JcrURLConnection)url.openConnection();

            final JcrURLConnection fConn = conn;
            Class<?> clazz;
            try
            {
               clazz = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                  public Class<?> run() throws Exception
                  {
                     return cl.parseClass(createCodeSource(fConn.getInputStream(), url.toString()));
                  }
               });
            }
            catch (PrivilegedActionException pae)
            {
               Throwable cause = pae.getCause();
               if (cause instanceof CompilationFailedException)
                  throw (CompilationFailedException)cause;
               else if (cause instanceof IOException)
                  throw (IOException)cause;
               else if (cause instanceof RuntimeException)
                  throw (RuntimeException)cause;
               else
                  throw new RuntimeException(cause);
            }
            classes[i] = clazz;
         }
         finally
         {
            if (conn != null)
               conn.disconnect();
         }
      }
      return classes;
   }

   /**
    * @return get underling groovy class loader
    */
   @Deprecated
   public GroovyClassLoader getGroovyClassLoader()
   {
      return classLoaderProvider.getGroovyClassLoader();
   }

   /**
    * Set groovy class loader.
    * 
    * @param gcl groovy class loader
    * @throws NullPointerException if <code>gcl == null</code>
    */
   @Deprecated
   public void setGroovyClassLoader(GroovyClassLoader gcl)
   {
      classLoaderProvider.setGroovyClassLoader(gcl);
   }

   /**
    * Create {@link GroovyCodeSource} from given stream and name. Code base
    * 'file:/groovy/script' (default code base used for all Groovy classes) will
    * be used.
    * 
    * @param in groovy source code stream
    * @param name code source name
    * @return GroovyCodeSource
    */
   // Override this method if need other behavior.
   protected GroovyCodeSource createCodeSource(final InputStream in, final String name)
   {
      GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
         public GroovyCodeSource run()
         {
            return new GroovyCodeSource(in, name, "/groovy/script");
         }
      });
      gcs.setCachable(false);
      return gcs;
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
