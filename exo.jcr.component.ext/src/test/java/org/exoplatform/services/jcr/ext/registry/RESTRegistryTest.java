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
package org.exoplatform.services.jcr.ext.registry;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ContainerResponseWriter;
import org.exoplatform.services.rest.GenericContainerResponse;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.security.ConversationState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

public class RESTRegistryTest extends BaseStandaloneTest
{

   private static final Log log = ExoLogger.getLogger(RESTRegistryTest.class);

   private ThreadLocalSessionProviderService sessionProviderService;

   private static final String SERVICE_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         + "<exo_service xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" jcr:primaryType=\"exo:registryEntry\"/>";

   private RESTRegistryService restRegService;

   private ResourceBinder binder;

   private RequestHandler handler;

   private URI baseUri;

   @Override
   public void setUp() throws Exception
   {

      super.setUp();
      this.sessionProviderService =
         (ThreadLocalSessionProviderService)container
            .getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      sessionProviderService.setSessionProvider(null, new SessionProvider(ConversationState.getCurrent()));
      restRegService = (RESTRegistryService)container.getComponentInstanceOfType(RESTRegistryService.class);
      binder = (ResourceBinder)container.getComponentInstanceOfType(ResourceBinder.class);
      handler = (RequestHandler)container.getComponentInstanceOfType(RequestHandler.class);

      baseUri = new URI("http://localhost:8080/rest");
   }

   public void testRESTRegservice() throws Exception
   {
      assertNotNull(restRegService);
      assertNotNull(binder);
      assertNotNull(handler);

      // List<ResourceClass> list = binder.getRootResources();
      // assertEquals(1, list.size());
      // assertEquals(3, list.get(0).getResourceMethods().size());

      log.info("-----REST-----");

      DummyContainerResponseWriter wr = new DummyContainerResponseWriter();
      URI reqUri = new URI(baseUri.toString() + "/registry/db1/");
      ContainerResponse cres =
         request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(200, cres.getStatus());
      log.info(new String(wr.getBody()));

      // request to exo:services/exo_service
      // response status should be 404 (NOT_FOUND)
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/exo_service");
      cres = request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(404, cres.getStatus());
      assertNull(wr.getBody());

      // create exo:services/exo_service
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES);
      cres =
         request(handler, wr, "POST", reqUri, baseUri, SERVICE_XML.getBytes(), new InputHeadersMap(
            new MultivaluedMapImpl()));
      assertEquals(201, cres.getStatus());
      assertEquals(new URI(reqUri + "/exo_service"), wr.getHeaders().getFirst(HttpHeaders.LOCATION));

      // request to exo:services/exo_service
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/exo_service");
      cres = request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(200, cres.getStatus());
      log.info(new String(wr.getBody()));

      // recreate exo:services/exo_service
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES);
      cres =
         request(handler, wr, "PUT", reqUri, baseUri, SERVICE_XML.getBytes(), new InputHeadersMap(
            new MultivaluedMapImpl()));
      assertEquals(201, cres.getStatus());
      assertEquals(new URI(reqUri + "/exo_service"), wr.getHeaders().getFirst(HttpHeaders.LOCATION));

      // delete exo:services/exo_service
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/exo_service");
      cres = request(handler, wr, "DELETE", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(204, cres.getStatus());

      // request to exo:services/exo_service
      // request status should be 404 (NOT_FOUND)
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/exo_service");
      cres = request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(404, cres.getStatus());
      assertNull(wr.getBody());

   }

   public void testCreateGetEntry() throws Exception
   {
      DummyContainerResponseWriter wr = new DummyContainerResponseWriter();
      InputStream in = new RegistryEntry("test").getAsInputStream();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int rd = -1;
      while ((rd = in.read()) != -1)
         out.write(rd);
      byte[] data = out.toByteArray();

      // check for exo:services/group/test
      // response status should be 404 (NOT_FOUND)
      URI reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/group/test");
      ContainerResponse cres =
         request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(404, cres.getStatus());
      assertNull(wr.getBody());
      // create exo:services/group/test
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/group/");
      cres = request(handler, wr, "POST", reqUri, baseUri, data, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(201, cres.getStatus());
      assertEquals(new URI(reqUri + "test"), wr.getHeaders().getFirst(HttpHeaders.LOCATION));

      // check again for exo:services/group/test
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/group/test");
      cres = request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(200, cres.getStatus());
      log.info(new String(wr.getBody()));

      // remove
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/group/");
      cres = request(handler, wr, "DELETE", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(204, cres.getStatus());

      // check for exo:services/group/test
      // response status should be 404 (NOT_FOUND)
      wr.reset();
      reqUri = new URI(baseUri.toString() + "/registry/db1/" + RegistryService.EXO_SERVICES + "/group/test");
      cres = request(handler, wr, "GET", reqUri, baseUri, null, new InputHeadersMap(new MultivaluedMapImpl()));
      assertEquals(404, cres.getStatus());
      assertNull(wr.getBody());

   }

   private static ContainerResponse request(RequestHandler handler, ContainerResponseWriter wr, String method,
      URI reqUri, URI baseUri, byte[] data, InputHeadersMap headers) throws Exception
   {
      InputStream in = data != null ? new ByteArrayInputStream(data) : null;
      ContainerRequest creq = new ContainerRequest(method, reqUri, baseUri, in, headers);
      ContainerResponse cres = new ContainerResponse(wr);
      handler.handleRequest(creq, cres);
      return cres;
   }

   public static class DummyContainerResponseWriter implements ContainerResponseWriter
   {

      private byte[] body;

      private MultivaluedMap<String, Object> headers;

      @SuppressWarnings("unchecked")
      public void writeBody(GenericContainerResponse response, MessageBodyWriter entityWriter) throws IOException
      {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         Object entity = response.getEntity();
         if (entity != null)
         {
            entityWriter.writeTo(entity, entity.getClass(), response.getEntityType(), null, response.getContentType(),
               response.getHttpHeaders(), out);
            body = out.toByteArray();
         }
      }

      public void writeHeaders(GenericContainerResponse response) throws IOException
      {
         headers = response.getHttpHeaders();
      }

      public byte[] getBody()
      {
         return body;
      }

      public MultivaluedMap<String, Object> getHeaders()
      {
         return headers;
      }

      public void reset()
      {
         body = null;
         headers = null;
      }

   }

}
