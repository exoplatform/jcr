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

import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RESTRegistryTest.DummyContainerResponseWriter;
import org.exoplatform.services.jcr.ext.resource.UnifiedNodeReference;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader.ScriptMetadata;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.ext.method.filter.MethodAccessFilter;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.services.rest.tools.DummySecurityContext;
import org.exoplatform.services.rest.tools.ResourceLauncher;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class GroovyScript2RestLoaderTest extends BaseGroovyTest
{

   private Node testRoot;

   private ResourceBinder binder;

   private ThreadLocalSessionProviderService sesProv;

   private Node scriptFile;

   private Node script;

   private Node groovyRepo;

   private int resourceNumber = 0;

   private ResourceLauncher launcher;

   private SecurityContext adminSecurityContext;

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
      RequestHandler handler = (RequestHandler)container.getComponentInstanceOfType(RequestHandler.class);

      testRoot = root.addNode("testRoot", "nt:unstructured");
      scriptFile = testRoot.addNode("script", "nt:file");
      script = scriptFile.addNode("jcr:content", "exo:groovyResourceContainer");
      script.setProperty("exo:autoload", true);
      script.setProperty("jcr:mimeType", "script/groovy");
      script.setProperty("jcr:lastModified", Calendar.getInstance());
      script
         .setProperty("jcr:data", Thread.currentThread().getContextClassLoader().getResourceAsStream("test1.groovy"));

      // repository for groovy dependencies
      groovyRepo = root.addNode("repo", "nt:folder");

      launcher = new ResourceLauncher(handler);

      ProviderBinder.setInstance(new ProviderBinder());
      ProviderBinder providers = ProviderBinder.getInstance();
      providers.addMethodInvokerFilter(new MethodAccessFilter());

      session.save();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");
      adminSecurityContext = new DummySecurityContext(new Principal() {
         public String getName()
         {
            return "root";
         }
      }, adminRoles);

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

   public void testGetMetatData() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Accept", MediaType.APPLICATION_JSON);
      ContainerResponse response =
         launcher.service("POST", "/script/groovy/meta/db1/ws/testRoot/script", "", headers, null, null);
      assertEquals(200, response.getStatus());
      ScriptMetadata data = (ScriptMetadata)response.getEntity();
      assertEquals("script/groovy", data.getMediaType());
      assertTrue(Boolean.valueOf(data.getLoad()));
   }

   public void testAutoload() throws Exception
   {

      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/autoload/db1/ws/testRoot/script?state=false", "", null, null, null);

      assertEquals(204, cres.getStatus());
      assertFalse(script.getProperty("exo:autoload").getBoolean());

      cres = launcher.service("POST", "/script/groovy/autoload/db1/ws/testRoot/script", "", null, null, null);

      assertEquals(204, cres.getStatus());
      assertTrue(script.getProperty("exo:autoload").getBoolean());
   }

   public void testLoad() throws Exception
   {
      EnvironmentContext ctx = new EnvironmentContext();
      ctx.put(SecurityContext.class, adminSecurityContext);
      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/load/db1/ws/testRoot/script?state=false", "", null, null, ctx);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber, binder.getSize());

      launcher.service("POST", "/script/groovy/load/db1/ws/testRoot/script", "", null, null, ctx);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber + 1, binder.getSize());
   }

   public void testLoadNoExtClassPath() throws Exception
   {
      String path = createScript(testRoot, "test.load", "Test000.groovy", //
         "import test.load.User000\n" + //
            "@javax.ws.rs.Path('test/load') class Test000 {\n" + //
            "def user = new User000(name:'test')\n" + //
            "@javax.ws.rs.GET def m() {user}" + //
            "}\n");
      EnvironmentContext ctx = new EnvironmentContext();
      ctx.put(SecurityContext.class, adminSecurityContext);

      int before = binder.getSize();
      ContainerResponse cres = launcher.service("POST", "/script/groovy/load/db1/ws" + path, "", null, null, ctx);
      assertEquals(400, cres.getStatus());
      int after = binder.getSize();
      assertEquals(before, after);
   }

   public void testLoadExtClassPath_File() throws Exception
   {
      String scriptPath = createScript(testRoot, "test.load", "Test001.groovy", //
         "import test.load.User001\n" + //
            "@javax.ws.rs.Path('test/load_001') class Test001 {\n" + //
            "def user = new User001(name:'test')\n" + //
            "@javax.ws.rs.GET def m() {user}" + //
            "}\n");
      String user = createScript(testRoot, "test.load", "User001.groovy",// 
         "package test.load\n" + //
            "class User001 {def name}");

      EnvironmentContext ctx = new EnvironmentContext();
      ctx.put(SecurityContext.class, adminSecurityContext);
      String path =
         "/script/groovy/load/db1/ws" + scriptPath //
            + "?file=" //
            + URLEncoder.encode(new UnifiedNodeReference(repository.getName(), workspace.getName(), user).getURL()
               .toString(), "UTF-8");
      int before = binder.getSize();
      ContainerResponse cres = launcher.service("POST", path, "", null, null, ctx);
      assertEquals(204, cres.getStatus());
      int after = binder.getSize();
      assertEquals(before + 1, after);
   }

   public void testLoadExtClassPath_SourceFolder() throws Exception
   {
      String scriptPath = createScript(testRoot, "test.load", "Test002.groovy", //
         "import test.load.User002\n" + //
            "@javax.ws.rs.Path('test/load_002') class Test002 {\n" + //
            "def user = new User002(name:'test')\n" + //
            "@javax.ws.rs.GET def m() {user}" + //
            "}\n");
      createScript(testRoot, "test.load", "User002.groovy",// 
         "package test.load\n" + //
            "class User002 {def name}");

      EnvironmentContext ctx = new EnvironmentContext();
      ctx.put(SecurityContext.class, adminSecurityContext);
      String path =
         "/script/groovy/load/db1/ws" + scriptPath //
            + "?sources=" //
            + URLEncoder.encode(new UnifiedNodeReference(repository.getName(), workspace.getName(), testRoot.getPath())
               .getURL().toString(), "UTF-8");
      int before = binder.getSize();
      ContainerResponse cres = launcher.service("POST", path, "", null, null, ctx);
      assertEquals(204, cres.getStatus());
      int after = binder.getSize();
      assertEquals(before + 1, after);
   }

   public void testDelete() throws Exception
   {
      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/delete/db1/ws/testRoot/script", "", null, null, null);

      assertEquals(204, cres.getStatus());
      assertEquals(resourceNumber, binder.getSize());
   }

   public void testGetScript() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Accept", "script/groovy");
      ByteArrayContainerResponseWriter wr = new ByteArrayContainerResponseWriter();
      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/src/db1/ws/testRoot/script", "", headers, null, wr, null);
      assertEquals(200, cres.getStatus());
      compareStream(script.getProperty("jcr:data").getStream(), new ByteArrayInputStream(wr.getBody()));
   }

   public void testAddScript() throws Exception
   {
      script.getParent().remove();
      session.save();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-type", "script/groovy");

      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/add/db1/ws/testRoot/script", "", headers,
            getResourceAsBytes("test1.groovy"), null, null);

      assertEquals(201, cres.getStatus());
   }

   public void testDispatchScript() throws Exception
   {
      ContainerResponse cres = launcher.service("GET", "/groovy-test/groovy1/test", "", null, null, null);

      assertEquals(200, cres.getStatus());
      assertEquals("Hello from groovy to test", cres.getEntity());

      // change script source code
      script
         .setProperty("jcr:data", Thread.currentThread().getContextClassLoader().getResourceAsStream("test2.groovy"));
      session.save();

      // must be rebounded , not created other one
      assertEquals(resourceNumber + 1, binder.getSize());

      cres = new ContainerResponse(new DummyContainerResponseWriter());
      cres = launcher.service("GET", "/groovy-test/groovy2/test", "", null, null, null);
      assertEquals(200, cres.getStatus());
      assertEquals("Hello from groovy to >>>>> test", cres.getEntity());
   }

   public void testValidate() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "public class Test { def a = 0\ndef =\n}\n";

      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/validate/%5Bno-name%5D", "", headers, script.getBytes(), null);
      assertEquals(400, cres.getStatus());

      script = "public class Test { def a = 0\ndef b = 1\n }\n";
      cres = launcher.service("POST", "/script/groovy/validate/%5Bno-name%5D", "", headers, script.getBytes(), null);
      assertEquals(200, cres.getStatus());
   }

   public void testValidateNoname() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "public class Test { def a = 0\ndef =\n}\n";

      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/validate/", "", headers, script.getBytes(), null);
      assertEquals(400, cres.getStatus());

      script = "public class Test { def a = 0\ndef b = 1\n }\n";
      cres = launcher.service("POST", "/script/groovy/validate/", "", headers, script.getBytes(), null);
      assertEquals(200, cres.getStatus());
   }

   public void testValidateNoExtClassPath() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "import test.validate.User000\n" + //
         "@javax.ws.rs.Path('test/validate') class Test {\n" + //
         "def user = new User000(name:'test')\n" + //
         "@javax.ws.rs.GET def m() {user}" + //
         " }\n";
      ContainerResponse cres =
         launcher.service("POST", "/script/groovy/validate/Test", "", headers, script.getBytes(), null);
      assertEquals(400, cres.getStatus());
   }

   public void testValidateExtClassPath_File() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "import test.validate.User001\n" + //
         "@javax.ws.rs.Path('test/validate') class Test {\n" + //
         "def user = new User001(name:'test')\n" + //
         "@javax.ws.rs.GET def m() {user}" + //
         " }\n";
      String user = createScript(testRoot, "test.validate", "User001.groovy",// 
         "package test.validate\n" + //
            "class User001 {def name}");
      // Specify source file location.
      String path =
         "/script/groovy/validate/Test?file=" //
            + URLEncoder.encode(new UnifiedNodeReference(repository.getName(), workspace.getName(), user).getURL()
               .toString(), "UTF-8");
      ContainerResponse cres = launcher.service("POST", path, "", headers, script.getBytes(), null);
      assertEquals(200, cres.getStatus());
   }

   public void testValidateExtClassPath_SourceFolder() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "script/groovy");
      String script = "import test.validate.User002\n" + //
         "@javax.ws.rs.Path('test/validate') class Test {\n" + //
         "def user = new User002(name:'test')\n" + //
         "@javax.ws.rs.GET def m() {user}" + //
         " }\n";
      createScript(testRoot, "test.validate", "User002.groovy",// 
         "package test.validate\n" + //
            "class User002 {def name}");
      // Specify source folder location.
      String path =
         "/script/groovy/validate/Test?sources=" //
            + URLEncoder.encode(new UnifiedNodeReference(repository.getName(), workspace.getName(), testRoot.getPath())
               .getURL().toString(), "UTF-8");
      ContainerResponse cres = launcher.service("POST", path, "", headers, script.getBytes(), null);
      assertEquals(200, cres.getStatus());
   }

   public void testGroovyDependency() throws Exception
   {
      // Add script in dependency repository
      createScript(groovyRepo, "dependencies", "Dep1.groovy", //
         "package dependencies\n" + //
            "class Dep1 { String name = getClass().getName() }");

      script.setProperty("jcr:data",
         Thread.currentThread().getContextClassLoader().getResourceAsStream("TestDependency.groovy"));

      session.save();

      // must be rebounded , not created other one
      assertEquals(resourceNumber + 1, binder.getSize());

      ContainerResponse cres = launcher.service("GET", "/groovy-test-dependency", "", null, null, null);
      assertEquals(200, cres.getStatus());
      assertEquals("dependencies.Dep1", cres.getEntity());
   }

   private byte[] getResourceAsBytes(String resource) throws IOException
   {
      byte[] data = null;
      InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
      if (stream != null)
      {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         byte[] buf = new byte[1024];
         int r = -1;
         while ((r = stream.read(buf)) != -1)
            bout.write(buf, 0, r);
         data = bout.toByteArray();
      }
      return data;
   }

}
