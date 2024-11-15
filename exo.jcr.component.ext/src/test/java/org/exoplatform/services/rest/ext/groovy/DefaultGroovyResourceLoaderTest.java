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

import org.exoplatform.services.rest.ext.BaseTest;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class DefaultGroovyResourceLoaderTest extends BaseTest
{
   private DefaultGroovyResourceLoader groovyResourceLoader;
   private URL root;
   
   public void setUp() throws Exception
   {
      super.setUp();
      root = Thread.currentThread().getContextClassLoader().getResource("repo");
      root = new URL(root.toString() + '/');
      groovyResourceLoader = new DefaultGroovyResourceLoader(root);
   }
   
   public void testLoadGroovySource() throws Exception
   {
      URL url = groovyResourceLoader.loadGroovySource("MyClass");
      assertNull(url);
      File f = new File(new URL(root, "MyClass.groovy").toURI());
      f.createNewFile();
      // Clean up data so that Unit Test can be executed several time
      f.deleteOnExit();
      url = groovyResourceLoader.loadGroovySource("MyClass");
      assertNotNull(url);
   }
}
