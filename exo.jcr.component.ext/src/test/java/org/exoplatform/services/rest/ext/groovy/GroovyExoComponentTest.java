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

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovyExoComponentTest extends BaseTest
{

   private InputStream script;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      container.registerComponentInstance(Component1.class.getName(), new Component1());
      script = Thread.currentThread().getContextClassLoader().getResourceAsStream("groovy2.groovy");
      assertNotNull(script);
   }

   @Override
   public void tearDown() throws Exception
   {
      container.unregisterComponent(Component1.class.getName());
      groovyPublisher.resources.clear();
      super.tearDown();
   }

   public void testExoComponentPerRequest() throws Exception
   {
      containerComponentTest(false, new BaseResourceId("g1"));
   }

   public void testExoComponentSingleton() throws Exception
   {
      containerComponentTest(true, new BaseResourceId("g2"));
   }

   private void containerComponentTest(boolean singleton, ResourceId resourceId) throws Exception
   {
      assertEquals(0, binder.getSize());
      assertEquals(0, groovyPublisher.resources.size());

      if (singleton)
         groovyPublisher.publishSingleton(script, resourceId, null);
      else
         groovyPublisher.publishPerRequest(script, resourceId, null);

      assertEquals(1, binder.getSize());
      assertEquals(1, groovyPublisher.resources.size());

      ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
      ContainerResponse resp = launcher.service("GET", "/a/b", "", null, null, writer, null);
      assertEquals(200, resp.getStatus());
      assertEquals("exo container's component", new String(writer.getBody()));
   }

   public static class Component1
   {
      public String getName()
      {
         return "exo container's component";
      }
   }

}
