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
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.Depth;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindResponseEntity;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

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

   private String propFindXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\">"
         + "<D:prop xmlns:webdav=\"http://www.exoplatform.org/jcr/webdav\">"
         + "<webdav:Author/><webdav:author/><webdave:DingALing/></D:prop></D:propfind>";

   private String propnameXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><propfind xmlns=\"DAV:\"><propname/></propfind>";

   private String allPropsXML =
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

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
      assertTrue(!find.contains("jcr:lockOnwer"));
      assertTrue(!find.contains("D:lockdiscovery"));
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
   

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
