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
package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RESTRegistryTest.DummyContainerResponseWriter;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader.ScriptMetadata;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class GroovyScript2RestLoaderTest extends BaseStandaloneTest
{

   private Node testRoot;

   private ResourceBinder binder;

   private RequestHandler handler;

   private ThreadLocalSessionProviderService sesProv;

   private Node scriptFile;

   private Node script;

   private int resourceNumber = 0;

   /**
    * {@inheritDoc}
    */
   public void setUp() throws Exception
   {
      super.setUp();

      sesProv =
         (ThreadLocalSessionProviderService)container
            .getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      sesProv.setSessionProvider(null, new SessionProvider(new ConversationState(new Identity("root"))));
      binder = (ResourceBinder)container.getComponentInstanceOfType(ResourceBinder.class);
      resourceNumber = binder.getSize();
      handler = (RequestHandler)container.getComponentInstanceOfType(RequestHandler.class);

      testRoot = root.addNode("testRoot", "nt:unstructured");
      scriptFile = testRoot.addNode("script", "nt:file");
      script = scriptFile.addNode("jcr:content", "exo:groovyResourceContainer");
      script.setProperty("exo:autoload", true);
      script.setProperty("jcr:mimeType", "script/groovy");
      script.setProperty("jcr:lastModified", Calendar.getInstance());
      script
         .setProperty("jcr:data", Thread.currentThread().getContextClassLoader().getResourceAsStream("test1.groovy"));
      session.save();
   }

   public void testStartQuery() throws Exception
   {
      String xpath = "//element(*, " + "exo:groovyResourceContainer" + ")[@exo:autoload='true']";
      Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
      QueryResult result = query.execute();
      assertEquals(1, result.getNodes().getSize());

      script.setProperty("exo:autoload", false);
      session.save();
      result = query.execute();
      assertEquals(0, result.getNodes().getSize());
   }

   public void testBindScripts() throws Exception
   {
      assertEquals(resourceNumber + 1, binder.getSize());
      script.getParent().remove();
      session.save();
      assertEquals(resourceNumber, binder.getSize());
   }

   public void testRemoteAccessGetMetatData() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Accept", MediaType.APPLICATION_JSON);
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/meta/db1/ws/testRoot/script"), new URI(""), null,
            new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());
      ScriptMetadata data = (ScriptMetadata)cres.getEntity();
      assertEquals("script/groovy", data.getMediaType());
      assertTrue(Boolean.valueOf(data.getLoad()));
   }

   public void testRemoteAccessAutoload() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/autoload/db1/ws/testRoot/script?state=false"),
            new URI(""), null, new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(204, cres.getStatus());
      assertFalse(script.getProperty("exo:autoload").getBoolean());
      creq =
         new ContainerRequest("POST", new URI("/script/groovy/autoload/db1/ws/testRoot/script"), new URI(""), null,
            new InputHeadersMap(headers));
      cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(204, cres.getStatus());
      assertTrue(script.getProperty("exo:autoload").getBoolean());
   }

   public void testRemoteAccessLoad() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/load/db1/ws/testRoot/script?state=false"), new URI(""),
            null, new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber, binder.getSize());
      creq =
         new ContainerRequest("POST", new URI("/script/groovy/load/db1/ws/testRoot/script"), new URI(""), null,
            new InputHeadersMap(headers));
      cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber + 1, binder.getSize());
   }

   public void testRemoteAccessDelete() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/delete/db1/ws/testRoot/script"), new URI(""), null,
            new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber, binder.getSize());
   }

   public void testRemoteAccessGetScript() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Accept", "script/groovy");
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/src/db1/ws/testRoot/script"), new URI(""), null,
            new InputHeadersMap(headers));
      ByteArrayContainerResponseWriter wr = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(wr);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());
      compareStream(script.getProperty("jcr:data").getStream(), new ByteArrayInputStream(wr.getBody()));
   }

   public void testRemoteAccessAddScript() throws Exception
   {
      script.getParent().remove();
      session.save();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-type", "script/groovy");
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/add/db1/ws/testRoot/script"), new URI(""), Thread
            .currentThread().getContextClassLoader().getResourceAsStream("test1.groovy"), new InputHeadersMap(headers));
      ByteArrayContainerResponseWriter wr = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(wr);
      handler.handleRequest(creq, cres);

      assertEquals(201, cres.getStatus());
   }

   public void testDispatchScript() throws Exception
   {
      ContainerRequest creq =
         new ContainerRequest("GET", new URI("/groovy-test/groovy1/test"), new URI(""), null, new InputHeadersMap(
            new MultivaluedMapImpl()));

      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());

      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());
      assertEquals("Hello from groovy to test", cres.getEntity());

      // change script source code
      script
         .setProperty("jcr:data", Thread.currentThread().getContextClassLoader().getResourceAsStream("test2.groovy"));
      session.save();

      // must be rebounded , not created other one
      assertEquals(resourceNumber + 1, binder.getSize());
      creq =
         new ContainerRequest("GET", new URI("/groovy-test/groovy2/test"), new URI(""), null, new InputHeadersMap(
            new MultivaluedMapImpl()));

      cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);
      assertEquals(200, cres.getStatus());
      assertEquals("Hello from groovy to >>>>> test", cres.getEntity());
   }

   public void testValidate() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "public class Test { def a = 0\ndef =\n}\n";
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/validate/%5Bno-name%5D"), new URI(""),
            new ByteArrayInputStream(script.getBytes()), new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);
      assertEquals(400, cres.getStatus());
      System.out.println(cres.getEntity());

      // cres.setResponse(null); //reset response
      script = "public class Test { def a = 0\ndef b = 1\n }\n";
      creq =
         new ContainerRequest("POST", new URI("/script/groovy/validate/%5Bno-name%5D"), new URI(""),
            new ByteArrayInputStream(script.getBytes()), new InputHeadersMap(headers));
      handler.handleRequest(creq, cres);
      assertEquals(200, cres.getStatus());
   }

   public void testValidateNoname() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "public class Test { def a = 0\ndef =\n}\n";
      ContainerRequest creq =
         new ContainerRequest("POST", new URI("/script/groovy/validate/"), new URI(""), new ByteArrayInputStream(script
            .getBytes()), new InputHeadersMap(headers));
      ContainerResponse cres = new ContainerResponse(new DummyContainerResponseWriter());
      handler.handleRequest(creq, cres);
      assertEquals(400, cres.getStatus());
      System.out.println(cres.getEntity());

      // cres.setResponse(null); //reset response
      script = "public class Test { def a = 0\ndef b = 1\n }\n";
      creq =
         new ContainerRequest("POST", new URI("/script/groovy/validate/"), new URI(""), new ByteArrayInputStream(script
            .getBytes()), new InputHeadersMap(headers));
      handler.handleRequest(creq, cres);
      assertEquals(200, cres.getStatus());
   }
}
