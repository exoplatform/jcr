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

import groovy.lang.GroovyObject;

import org.exoplatform.services.rest.ext.BaseTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
@SuppressWarnings("rawtypes")
public class ExtendedClassLoaderTest extends BaseTest
{
   public void testParseClasses() throws Exception
   {
      ExtendedGroovyClassLoader loader = new GroovyClassLoaderProvider().getGroovyClassLoader();
      SourceFile[] sources = new SourceFile[2];
      sources[0] = new SourceFile(Thread.currentThread().getContextClassLoader().getResource("GMain1.groovy"));
      sources[1] =
         new SourceFile(Thread.currentThread().getContextClassLoader().getResource("repo/dependencies/Dep1.groovy"));
      Class[] classes = loader.parseClasses(sources);
      assertEquals(2, classes.length);
      List<String> names = new ArrayList<String>(2);
      for (Class c : classes)
         names.add(c.getName());
      assertTrue(names.contains("GMain1"));
      assertTrue(names.contains("dependencies.Dep1"));
   }

   public void testParseClasses2() throws Exception
   {
      ExtendedGroovyClassLoader loader =
         new GroovyClassLoaderProvider().getGroovyClassLoader(new SourceFolder[]{new SourceFolder(Thread
            .currentThread().getContextClassLoader().getResource("repo"))});
      SourceFile[] sources = new SourceFile[1];
      sources[0] = new SourceFile(Thread.currentThread().getContextClassLoader().getResource("GMain1.groovy"));
      Class[] classes = loader.parseClasses(sources);
      assertEquals(1, classes.length);
      List<String> names = new ArrayList<String>(1);
      for (Class c : classes)
         names.add(c.getName());
      assertTrue(names.contains("GMain1"));
   }

   public void testParseClassWithDependency() throws Exception
   {
      ExtendedGroovyClassLoader loader = new GroovyClassLoaderProvider().getGroovyClassLoader();
      SourceFile[] sources =
         new SourceFile[]{new SourceFile(Thread.currentThread().getContextClassLoader()
            .getResource("repo/dependencies/Dep1.groovy"))};
      Class clazz =
         loader.parseClass(Thread.currentThread().getContextClassLoader().getResourceAsStream("GMain1.groovy"),
            "GMain1", sources);
      assertEquals("GMain1", clazz.getName());
      assertEquals("dependencies.Dep1", ((GroovyObject)clazz.newInstance()).invokeMethod("m0", new Object[0]));
   }
}
