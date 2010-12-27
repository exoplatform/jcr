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

import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.rest.ext.groovy.SourceFolder;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovyCompilerTest extends BaseGroovyTest
{

   private Node groovyRepo;

   private Node otherGroovyRepo;

   private String scriptA;

   private String scriptB;

   public void setUp() throws Exception
   {
      super.setUp();
      groovyRepo = root.addNode("groovyRepo", "nt:folder");
      otherGroovyRepo = root.addNode("otherGroovyRepo", "nt:folder");

      // Add script in shared "dependency repository".
      scriptA = createScript(groovyRepo, "org.exoplatform", "A.groovy", //
         "package org.exoplatform\n" + //
            "class A { String message = 'groovy compiler test' }");

      scriptB = createScript(groovyRepo, "org.exoplatform.test", "B.groovy", //
         "package org.exoplatform.test\n" + //
            "import org.exoplatform.A\n" + //
            "class B extends A {}");
   }

   public void testSnaredDependencies() throws Exception
   {
      JcrGroovyClassLoaderProvider classLoaderProvider = new JcrGroovyClassLoaderProvider();
      classLoaderProvider.getGroovyClassLoader().setResourceLoader(
         new JcrGroovyResourceLoader(new java.net.URL[]{new java.net.URL("jcr://db1/ws#/groovyRepo")}));
      JcrGroovyCompiler compiler = new JcrGroovyCompiler(classLoaderProvider);

      Class<?>[] classes = compiler.compile(new UnifiedNodeReference("db1", "ws", scriptB));
      assertEquals(1, classes.length);
      GroovyObject go = (GroovyObject)classes[0].newInstance();
      assertEquals("groovy compiler test", go.invokeMethod("getMessage", new Object[0]));
   }

   public void testDependenciesBetweenCompiled() throws Exception
   {
      JcrGroovyClassLoaderProvider classLoaderProvider = new JcrGroovyClassLoaderProvider();
      JcrGroovyCompiler compiler = new JcrGroovyCompiler(classLoaderProvider);
      Class<?>[] classes =
         compiler.compile(new UnifiedNodeReference("db1", "ws", scriptB),
            new UnifiedNodeReference("db1", "ws", scriptA));
      assertEquals(2, classes.length);
      GroovyObject go = (GroovyObject)classes[0].newInstance();
      assertEquals("groovy compiler test", go.invokeMethod("getMessage", new Object[0]));
      go = (GroovyObject)classes[1].newInstance();
      assertEquals("groovy compiler test", go.invokeMethod("getMessage", new Object[0]));
   }

   public void testAddDependenciesInRuntime() throws Exception
   {
      JcrGroovyClassLoaderProvider classLoaderProvider = new JcrGroovyClassLoaderProvider();
      JcrGroovyCompiler compiler = new JcrGroovyCompiler(classLoaderProvider);
      SourceFolder[] src =
         new SourceFolder[]{new SourceFolder(new UnifiedNodeReference("db1", "ws", groovyRepo.getPath()).getURL())};
      Class<?>[] classes = compiler.compile(src, new UnifiedNodeReference("db1", "ws", scriptB));
      assertEquals(1, classes.length);
      GroovyObject go = (GroovyObject)classes[0].newInstance();
      assertEquals("groovy compiler test", go.invokeMethod("getMessage", new Object[0]));
   }

   public void testCombinedDependencies() throws Exception
   {
      // Add file without respect to package structure to be sure direct link to source files works any way.
      String scriptC = createScript(otherGroovyRepo, "org.exoplatform", "C", //
         "package org.exoplatform.test\n" + //
            "import org.exoplatform.*\n" + //
            "class C extends B {}");

      String scriptD = createScript(otherGroovyRepo, "org.exoplatform.test.other", "D.groovy", //
         "package org.exoplatform.test.other\n" + //
            "import org.exoplatform.test.C\n" + //
            "class D extends C {}");

      JcrGroovyClassLoaderProvider classLoaderProvider = new JcrGroovyClassLoaderProvider();
      classLoaderProvider.getGroovyClassLoader().setResourceLoader(
         new JcrGroovyResourceLoader(new java.net.URL[]{new java.net.URL("jcr://db1/ws#/groovyRepo")}));

      JcrGroovyCompiler compiler = new JcrGroovyCompiler(classLoaderProvider);
      Class<?>[] classes =
         compiler.compile(new UnifiedNodeReference("db1", "ws", scriptD),
            new UnifiedNodeReference("db1", "ws", scriptC));
      assertEquals(2, classes.length);
      List<String> names = new ArrayList<String>(2);
      for (Class c : classes)
         names.add(c.getName());
      assertTrue(names.contains("org.exoplatform.test.C"));
      assertTrue(names.contains("org.exoplatform.test.other.D"));
      //System.out.println(">>>>>>>>>>>>>>>>> "+names);      
   }
}
