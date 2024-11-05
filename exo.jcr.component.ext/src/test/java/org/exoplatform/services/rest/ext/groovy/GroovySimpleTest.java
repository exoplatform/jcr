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

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class GroovySimpleTest extends BaseTest
{

   @Override
   public void tearDown() throws Exception
   {
      groovyPublisher.resources.clear();
      super.tearDown();
   }

   public void testPerRequest() throws Exception
   {
      publicationTest(false, new BaseResourceId("g1"));
   }

   public void testSingleton() throws Exception
   {
      publicationTest(true, new BaseResourceId("g2"));
   }

   private void publicationTest(boolean singleton, ResourceId resourceId) throws Exception
   {
      String script = //
         "@javax.ws.rs.Path(\"a\")" //
            + "class GroovyResource {" //
            + "@javax.ws.rs.GET @javax.ws.rs.Path(\"{who}\")" //
            + "def m0(@javax.ws.rs.PathParam(\"who\") String who) { return (\"hello \" + who)}" //
            + "}";

      assertEquals(0, binder.getSize());
      assertEquals(0, groovyPublisher.resources.size());

      if (singleton)
         groovyPublisher.publishSingleton(script, resourceId, null);
      else
         groovyPublisher.publishPerRequest(script, resourceId, null);

      assertEquals(1, binder.getSize());
      assertEquals(1, groovyPublisher.resources.size());
      assertTrue(groovyPublisher.isPublished(resourceId));

      String cs =
         binder.getResources().get(0).getObjectModel().getObjectClass().getProtectionDomain().getCodeSource()
            .getLocation().toString();
      assertEquals("file:/groovy/script/jaxrs", cs);

      ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
      ContainerResponse resp = launcher.service("GET", "/a/groovy", "", null, null, writer, null);
      assertEquals(200, resp.getStatus());
      assertEquals("hello groovy", new String(writer.getBody()));

      groovyPublisher.unpublishResource(resourceId);

      assertEquals(0, binder.getSize());
      assertEquals(0, groovyPublisher.resources.size());
   }

}
