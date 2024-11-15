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
package org.exoplatform.services.script.groovy.jarjar;

import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyObject;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestScript
{

   /** . */
   private final String name;

   public TestScript(String name)
   {
      this.name = name;
   }

   public Object execute(Mapping mapping)
   {

      //
      JarJarClassLoader loader =
         JarJarClassLoader.createJarJarClassLoaderInPrivilegedMode(Thread.currentThread().getContextClassLoader());

      //
      mapping.configure(loader);

      // Obtain script class
      URL url = Thread.currentThread().getContextClassLoader().getResource("jarjar/" + name);
      Assert.assertNotNull(url);
      GroovyCodeSource gcs;
      try
      {
         gcs = new GroovyCodeSource(url);
      }
      catch (Exception e)
      {
         AssertionFailedError err = new AssertionFailedError();
         err.initCause(e);
         throw err;
      }

      Class testClass = loader.parseClass(gcs);;

      // Instantiate script
      GroovyObject testObject;
      try
      {
         testObject = (GroovyObject)testClass.newInstance();
      }
      catch (Exception e)
      {
         AssertionFailedError err = new AssertionFailedError();
         err.initCause(e);
         throw err;
      }

      // Invoke finally
      return testObject.invokeMethod("run", new Object[0]);
   }
}
