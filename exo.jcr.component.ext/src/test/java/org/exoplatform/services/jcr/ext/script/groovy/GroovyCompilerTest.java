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

import groovy.lang.GroovyObject;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;

import java.util.Calendar;

import javax.jcr.Node;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovyCompilerTest extends BaseStandaloneTest
{

   public void setUp() throws Exception
   {
      super.setUp();
      Node groovyRepo = root.addNode("groovyRepo", "nt:folder");
      Node org = groovyRepo.addNode("org", "nt:folder");
      Node exo = org.addNode("exoplatform", "nt:folder");
      Node a = exo.addNode("A.groovy", "nt:file");
      a = a.addNode("jcr:content", "nt:resource");
      a.setProperty("jcr:mimeType", "script/groovy");
      a.setProperty("jcr:lastModified", Calendar.getInstance());
      a.setProperty("jcr:data", //
         "package org.exoplatform\n" + //
            " class A { String message = 'groovy compiler test' }");

      Node test = exo.addNode("test", "nt:folder");
      Node b = test.addNode("B.groovy", "nt:file");
      b = b.addNode("jcr:content", "nt:resource");
      b.setProperty("jcr:mimeType", "script/groovy");
      b.setProperty("jcr:lastModified", Calendar.getInstance());
      b.setProperty("jcr:data", //
         "package org.exoplatform.test\n" + //
            " import org.exoplatform.A\n" + //
            " class B extends A {}");
      session.save();
   }

   public void testGroovyDependency() throws Exception
   {
      JcrGroovyCompiler compiler = new JcrGroovyCompiler();
      compiler.getGroovyClassLoader().setResourceLoader(
         new JcrGroovyResourceLoader(new java.net.URL[]{new java.net.URL("jcr://db1/ws#/groovyRepo")}));
      Class<?>[] classes =
         compiler.compile(new UnifiedNodeReference("db1", "ws", "/groovyRepo/org/exoplatform/test/B.groovy"));
      assertEquals(1, classes.length);
      GroovyObject go = (GroovyObject)classes[0].newInstance();
      assertEquals("groovy compiler test", go.invokeMethod("getMessage", new Object[0]));
   }

}
