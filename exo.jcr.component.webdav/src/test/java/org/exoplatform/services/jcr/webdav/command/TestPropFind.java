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
package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.Depth;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.acl.ACLProperties;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindResponseEntity;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ext.provider.HierarchicalPropertyEntityProvider;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.tools.DummySecurityContext;
import org.exoplatform.services.rest.tools.ResourceLauncher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.namespace.QName;
/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestPropFind extends BaseStandaloneTest
{

   protected Node testPropFind;

   private final String author = "eXoPlatform";

   private final String authorProp = "webdav:Author";

   private final String nt_webdave_file = "webdav:file";

   private final String USER_ROOT = "root";

   private final String USER_JOHN = "john";

   private final String BASE_URI = "http://localhost";

   private String propFindXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\">"
         + "<D:prop xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">"
         + "<webdav:Author/><webdav:author/><webdave:DingALing/></D:prop></D:propfind>";

   private String propnameXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><propfind xmlns=\"DAV:\"><propname/></propfind>";

   private String allPropsXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

   private String allPropsWithInclusionXML = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\">"
      + "<D:allprop/><D:include><D:lockdiscovery/><D:supported-method-set/></D:include></D:propfind>";

   public void setUp() throws Exception
   {
      super.setUp();
      testPropFind = root.addNode("TestPropFind", "nt:folder");
   }

   public void testPropfindComplexContent() throws Exception
   {

      String path = testPropFind.getPath() + "/testPropfindComplexContent";

      // prepare data
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream("file content".getBytes()), "nt:file",
            "exo:testResource", "text/plain");

      node.getNode("jcr:content").addNode("node", "nt:unstructured").setProperty("node-prop", "prop");
      node.getNode("jcr:content").setProperty("exo:prop", "prop");

      // test
      HierarchicalProperty body = new HierarchicalProperty("D:propfind", null, "DAV:");
      body.addChild(new HierarchicalProperty("D:allprop", null, "DAV:"));
      Response resp = new PropFindCommand().propfind(session, path, body, Depth.INFINITY_VALUE, "http://localhost");

      ByteArrayOutputStream bas = new ByteArrayOutputStream();
      ((PropFindResponseEntity)resp.getEntity()).write(bas);
      System.out.println(">>>>>>>>>>RESSSSSSSSSSSP>>>>>>>>>>>>>>> " + new String(bas.toByteArray()));

   }

   public void testSimplePropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      ContainerResponse containerResponseFind = service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, null);
      assertEquals(HTTPStatus.MULTISTATUS, containerResponseFind.getStatus());
   }

   /**
    * Here we test WebDAV PROPFIND method implementation for correct response 
    * if request contains encoded non-latin characters. We send a request with
    * corresponding character sequence and expect to receive response containing
    * 'href' element with URL encoded characters and 'displayname' element containing
    * non-latin characters.  
    * @throws Exception
    */
   public void testSimplePropFindWithNonLatin() throws Exception
   {
      // prepare file name and content
      String encodedfileName = "%e3%81%82%e3%81%84%e3%81%86%e3%81%88%e3%81%8a";
      String decodedfileName = URLDecoder.decode(encodedfileName, "UTF-8");
      String content = TestUtils.getFileContent();
      TestUtils.addContent(session, decodedfileName, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      TestUtils.addNodeProperty(session, decodedfileName, authorProp, author);

      ContainerResponse response =
         service(WebDAVMethods.PROPFIND, getPathWS() + "/" + encodedfileName, "", null, allPropsXML.getBytes());

      // serialize response entity to string
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)response.getEntity();
      entity.write(outputStream);
      String resp = outputStream.toString("UTF-8");

      System.out.println("=======PropFind response==========");
      System.out.println(resp);
      System.out.println("=======Decoded file name==========");
      System.out.println(decodedfileName);
      System.out.println("==================================");

      assertTrue(resp.contains(encodedfileName));
      assertTrue(resp.contains(decodedfileName));
   }

   public void testPropFind() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      TestUtils.addNodeProperty(session, file, authorProp, author);
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, "text/xml");
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", headers, propFindXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains(authorProp));
      assertTrue(find.contains(author));
   }

   public void testPropNames() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      TestUtils.addNodeProperty(session, file, authorProp, author);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, propnameXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains(authorProp));
      assertTrue(find.contains("D:getlastmodified"));
      assertFalse(find.contains("jcr:lockOwner"));
      assertFalse(find.contains("D:lockdiscovery"));
   }

   public void testAllProps() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      TestUtils.addNodeProperty(session, file, authorProp, author);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, allPropsXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains("D:getlastmodified"));
      assertTrue(find.contains(authorProp));
      assertTrue(find.contains(author));
   }
 
   public void testAllPropsWithInclusion() throws Exception
   {

      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      Node node =
         TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");

      node.addMixin("mix:lockable");
      node.save();
      node.lock(true, false);
      node.getPath();
      node.getName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, "text/xml");
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, allPropsWithInclusionXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      System.out.println("\n" + find);
      assertTrue(find.contains("D:lockdiscovery"));
      assertTrue(find.contains("D:supported-method-set"));

   }
   
   public void testPropfindWrongDataFormat() throws Exception
   {

      String path = testPropFind.getPath() + "/testPropfindComplexContent";
   
         // prepare data
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream("file content".getBytes()), "nt:file",
            "exo:testResource", "text/plain");

      node.addMixin("mix:lockable");
      node.save();
      node.lock(false, false);
      node.getNode("jcr:content").addMixin("mix:lockable");
      node.save();
      node.getNode("jcr:content").lock(true, false);
      node.getNode("jcr:content").addNode("node", "nt:unstructured").setProperty("node-prop", "prop");
      node.getNode("jcr:content").setProperty("exo:prop", "prop");
      node.save();

      // test
      HierarchicalProperty body = new HierarchicalProperty("D:propfind", null, "DAV:");
      body.addChild(new HierarchicalProperty("D:allprop", null, "DAV:"));
      Response resp = new PropFindCommand().propfind(session, path, body, Depth.INFINITY_VALUE, "http://localhost");
      ByteArrayOutputStream bas = new ByteArrayOutputStream();
      ((PropFindResponseEntity)resp.getEntity()).write(bas);
      String find = new String(bas.toByteArray());
      assertFalse(find.contains("jcr:lockOnwer"));
      assertFalse(find.contains("D:lockdiscovery"));
   }
   
   public void testPropWithPercent() throws Exception
   {
      String content = TestUtils.getFileContent();
      String file = TestUtils.getFileName();
      TestUtils.addContent(session, file, new ByteArrayInputStream(content.getBytes()), nt_webdave_file, "");
      String authorValue = "bla % bla";
      TestUtils.addNodeProperty(session, file, authorProp, authorValue);
      ContainerResponse responseFind =
         service(WebDAVMethods.PROPFIND, getPathWS() + file, "", null, allPropsXML.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, responseFind.getStatus());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PropFindResponseEntity entity = (PropFindResponseEntity)responseFind.getEntity();
      entity.write(outputStream);
      String find = outputStream.toString();
      assertTrue(find.contains("D:getlastmodified"));
      assertTrue(find.contains(authorProp));
      assertTrue(find.contains(authorValue));
   }
   
   /**
    * Here we check for correct response for PROPFIND request.
    * Response should not only contain an acl element with its properties
    * (ace, principle, privelege, grant, etc.) but also be correctly composed and contain
    * ACL information about user root.
    * @throws Exception
    */
   public void testPropfindPermissionsOnRoot() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "0");
      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:propfind xmlns:D=\"DAV:\">" + "<D:prop>" + "<D:owner/>"
            + "<D:acl/>" + "</D:prop>" + "</D:propfind>";

      ContainerResponse response =
         launcher.service(WebDAVMethods.PROPFIND, getPathWS(), BASE_URI, headers, request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      assertNotNull(response.getEntity());

      HierarchicalPropertyEntityProvider provider = new HierarchicalPropertyEntityProvider();
      InputStream inputStream = TestUtils.getResponseAsStream(response);
      HierarchicalProperty multistatus = provider.readFrom(null, null, null, null, null, inputStream);

      assertEquals(new QName("DAV:", "multistatus"), multistatus.getName());
      assertEquals(1, multistatus.getChildren().size());

      HierarchicalProperty resourceProp = multistatus.getChildren().get(0);

      HierarchicalProperty resourceHref = resourceProp.getChild(new QName("DAV:", "href"));
      assertNotNull(resourceHref);
      assertEquals(BASE_URI + getPathWS() + "/", resourceHref.getValue());

      HierarchicalProperty propstatProp = resourceProp.getChild(new QName("DAV:", "propstat"));
      HierarchicalProperty propProp = propstatProp.getChild(new QName("DAV:", "prop"));

      HierarchicalProperty ownerProp = propProp.getChild(new QName("DAV:", "owner"));
      HierarchicalProperty ownerHrefProp = ownerProp.getChild(new QName("DAV:", "href"));

      assertEquals("__system", ownerHrefProp.getValue());

      HierarchicalProperty aclProp = propProp.getChild(ACLProperties.ACL);
      assertEquals(1, aclProp.getChildren().size());

      HierarchicalProperty aceProp = aclProp.getChild(ACLProperties.ACE);
      assertEquals(2, aceProp.getChildren().size());

      HierarchicalProperty principalProp = aceProp.getChild(ACLProperties.PRINCIPAL);
      assertEquals(1, principalProp.getChildren().size());

      HierarchicalProperty allProp = principalProp.getChild(ACLProperties.ALL);
      assertNotNull(allProp);

      HierarchicalProperty grantProp = aceProp.getChild(ACLProperties.GRANT);
      assertEquals(2, grantProp.getChildren().size());

      HierarchicalProperty writeProp = grantProp.getChild(0).getChild(ACLProperties.WRITE);
      assertNotNull(writeProp);
      HierarchicalProperty readProp = grantProp.getChild(1).getChild(ACLProperties.READ);
      assertNotNull(readProp);
   }

   /**
    * Here we check for correct response for PROPFIND request.
    * Response should contain all available acl information on current node, i.e.
    * ace for user "__system", "john" etc.
    * @throws Exception
    */
   public void testPropfindPropOwnerAndAclOnNode() throws Exception
   {

      NodeImpl testNode = (NodeImpl)root.addNode("test_acl_property", "nt:folder");
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      session.save();

      Map<String, String[]> permissions = new HashMap<String, String[]>();

      String userName = USER_JOHN;
      permissions.put(userName, PermissionType.ALL);

      testNode.setPermissions(permissions);
      testNode.getSession().save();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Depth", "1");
      headers.putSingle(HttpHeaders.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

      EnvironmentContext ctx = new EnvironmentContext();

      Set<String> adminRoles = new HashSet<String>();
      adminRoles.add("administrators");

      DummySecurityContext adminSecurityContext = new DummySecurityContext(new Principal()
      {
         public String getName()
         {
            return USER_ROOT;
         }
      }, adminRoles);

      ctx.put(SecurityContext.class, adminSecurityContext);

      RequestHandlerImpl handler = (RequestHandlerImpl)container.getComponentInstanceOfType(RequestHandlerImpl.class);
      ResourceLauncher launcher = new ResourceLauncher(handler);

      String request =
         "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:propfind xmlns:D=\"DAV:\">" + "<D:prop>" + "<D:owner/>"
            + "<D:acl/>" + "</D:prop>" + "</D:propfind>";

      ContainerResponse cres =
         launcher.service(WebDAVMethods.PROPFIND, getPathWS() + testNode.getPath(), BASE_URI, headers,
            request.getBytes(), null, ctx);

      assertEquals(HTTPStatus.MULTISTATUS, cres.getStatus());

      HierarchicalPropertyEntityProvider provider = new HierarchicalPropertyEntityProvider();
      InputStream inputStream = TestUtils.getResponseAsStream(cres);
      HierarchicalProperty multistatus = provider.readFrom(null, null, null, null, null, inputStream);

      assertEquals(new QName("DAV:", "multistatus"), multistatus.getName());
      assertEquals(1, multistatus.getChildren().size());

      HierarchicalProperty resourceProp = multistatus.getChildren().get(0);

      HierarchicalProperty resourceHref = resourceProp.getChild(new QName("DAV:", "href"));
      assertNotNull(resourceHref);
      assertEquals(BASE_URI + getPathWS() + testNode.getPath() + "/", resourceHref.getValue());

      HierarchicalProperty propstatProp = resourceProp.getChild(new QName("DAV:", "propstat"));
      HierarchicalProperty propProp = propstatProp.getChild(new QName("DAV:", "prop"));

      HierarchicalProperty aclProp = propProp.getChild(ACLProperties.ACL);
      assertEquals(1, aclProp.getChildren().size());

      HierarchicalProperty aceProp = aclProp.getChild(ACLProperties.ACE);
      assertEquals(2, aceProp.getChildren().size());

      HierarchicalProperty principalProp = aceProp.getChild(ACLProperties.PRINCIPAL);
      assertEquals(1, principalProp.getChildren().size());

      assertEquals(userName, principalProp.getChildren().get(0).getValue());

      HierarchicalProperty grantProp = aceProp.getChild(ACLProperties.GRANT);
      assertEquals(2, grantProp.getChildren().size());

      HierarchicalProperty writeProp = grantProp.getChild(0).getChild(ACLProperties.WRITE);
      assertNotNull(writeProp);
      HierarchicalProperty readProp = grantProp.getChild(1).getChild(ACLProperties.READ);
      assertNotNull(readProp);

   }

   /**
    * Here we're testing the case when we are trying to get some resource C property to a path /A/B/C
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testPropFindForNonExistingWorkspace() throws Exception
   {
      String file = TestUtils.getFileName();

      ContainerResponse response =
         service(WebDAVMethods.PROPFIND, getPathWS() + "_" + file, "", null, null);
      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
