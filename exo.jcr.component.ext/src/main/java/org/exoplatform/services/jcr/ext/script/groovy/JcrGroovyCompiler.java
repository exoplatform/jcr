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
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.ext.resource.JcrURLConnection;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * JcrGroovyCompiler can load source code of groovy script from JCR and parse it
 * via GroovyClassLoader.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JcrGroovyCompiler
{
   protected GroovyClassLoader gcl;

   public JcrGroovyCompiler()
   {
      this.gcl = SecurityHelper.doPriviledgedAction(new PrivilegedAction<GroovyClassLoader>()
      {
         public GroovyClassLoader run()
         {
            return new GroovyClassLoader(getClass().getClassLoader());
         }
      });
   }

   /**
    * @return get underling groovy class loader
    */
   public GroovyClassLoader getGroovyClassLoader()
   {
      return gcl;
   }

   /**
    * Set groovy class loader.
    *
    * @param gcl groovy class loader
    * @throws NullPointerException if <code>gcl == null</code>
    */
   public void setGroovyClassLoader(GroovyClassLoader gcl)
   {
      if (gcl == null)
         throw new NullPointerException("GroovyClassLoader may not be null.");
      this.gcl = gcl;
   }

   public Class<?>[] compile(UnifiedNodeReference... sourceReferences) throws IOException
   {
      final GroovyClassLoader cl = gcl;
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
               clazz = SecurityHelper.doPriviledgedExceptionAction(new PrivilegedExceptionAction<Class<?>>()
               {
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
               {
                  throw (CompilationFailedException)cause;
               }
               else if (cause instanceof IOException)
               {
                  throw (IOException)cause;
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
            classes[i] = clazz;
         }
         finally
         {
            if (conn != null)
            {
               conn.disconnect();
            }
         }
      }
      return classes;
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
      GroovyCodeSource gcs = SecurityHelper.doPriviledgedAction(new PrivilegedAction<GroovyCodeSource>()
      {
         public GroovyCodeSource run()
         {
            return new GroovyCodeSource(in, name, "/groovy/script");
         }
      });

      gcs.setCachable(false);
      return gcs;
   }
}
