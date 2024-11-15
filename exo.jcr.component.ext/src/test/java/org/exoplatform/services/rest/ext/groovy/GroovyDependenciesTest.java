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
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;

import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovyDependenciesTest extends BaseTest
{
   private InputStream script;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      URL root = Thread.currentThread().getContextClassLoader().getResource("repo");
      DefaultGroovyResourceLoader groovyResourceLoader = new DefaultGroovyResourceLoader(root);
      groovyPublisher.getGroovyClassLoader().setResourceLoader(groovyResourceLoader);

      script = Thread.currentThread().getContextClassLoader().getResourceAsStream("GMain1.groovy");
      assertNotNull(script);
   }

   public void testDependency() throws Exception
   {
      groovyPublisher.publishPerRequest(script, new BaseResourceId("GMain1"), null);
      ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
      ContainerResponse resp = launcher.service("GET", "/a", "", null, null, writer, null);
      assertEquals(200, resp.getStatus());
      assertEquals("dependencies.Dep1", new String(writer.getBody()));
   }

   @Override
   public void tearDown() throws Exception
   {
      groovyPublisher.resources.clear();
      super.tearDown();
   }
}
