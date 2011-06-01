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
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.WebDavServiceImpl;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestPut extends BaseStandaloneTest
{

   public void testPut() throws Exception
   {
      String content = TestUtils.getFileContent();
      String path = TestUtils.getFileName();
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + path, "", null, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(path)));
   }

   public void testPutNotFound() throws Exception
   {
      String content = TestUtils.getFileContent();
      String path = TestUtils.getFileName();
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + "/not-found" + path, "", null, content.getBytes());
      assertEquals(HTTPStatus.CONFLICT, containerResponse.getStatus());

   }

   /**
    * Here we're testing the case when we are trying to put some resource C to a path /A/B/C
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testPutToNonExistingWorkspace() throws Exception
   {
      String content = TestUtils.getFileContent();
      String path = TestUtils.getFileName();
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + "_" + path, "", null, content.getBytes());
      assertEquals(HTTPStatus.CONFLICT, containerResponse.getStatus());

   }

   public void testPutFileContentTypeHeader() throws Exception
   {
      String content = TestUtils.getFileContent();

      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + TestUtils.getFileName(), "", null, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.FILE_NODETYPE, "nt:folder");
      containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + TestUtils.getFileName(), "", headers, content.getBytes());
      assertEquals(HTTPStatus.BAD_REQUEST, containerResponse.getStatus());

      String fileName = TestUtils.getFileName();
      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.FILE_NODETYPE, "nt:file");
      containerResponse = service(WebDAVMethods.PUT, getPathWS() + fileName, "", headers, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      assertEquals("nt:file", TestUtils.getFileNodeType(session, fileName));

   }

   public void testPutContentTypeHeader() throws Exception
   {
      String content = TestUtils.getFileContent();
      String fileName = TestUtils.getFileName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENT_NODETYPE, "webdav:goodres");
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + fileName, "", headers, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      assertEquals("webdav:goodres", TestUtils.getContentNodeType(session, fileName));

      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENT_NODETYPE, "webdav:badres");
      containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + TestUtils.getFileName(), "", headers, content.getBytes());
      assertEquals(HTTPStatus.BAD_REQUEST, containerResponse.getStatus());

   }

   public void testPutMixinsHeader() throws Exception
   {
      String content = TestUtils.getFileContent();
      String fileName = TestUtils.getFileName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.CONTENT_NODETYPE, "webdav:goodres");
      headers.add(ExtHttpHeaders.CONTENT_MIXINTYPES, "mix:wdTestMixin1,mix:wdTestMixin2");
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + fileName, "", headers, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      NodeType[] mixins = TestUtils.getContentMixins(session, fileName);

      assertEquals(2, mixins.length);
      
      for (NodeType mixin : mixins)
      {
         assertTrue(mixin.getName().equals("mix:wdTestMixin1") || mixin.getName().equals("mix:wdTestMixin2"));
      }

   }

   public void testMimeType() throws Exception
   {
      String content = TestUtils.getFileContent();
      String path = TestUtils.getFileName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML);
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + path, "", headers, content.getBytes());
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(path)));
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      assertTrue(node.hasNode("jcr:content"));
      Node node2 = node.getNode("jcr:content");
      assertTrue(node2.hasProperty("jcr:mimeType"));
      PropertyImpl property = (PropertyImpl)node2.getProperty("jcr:mimeType");
      assertEquals(headers.getFirst(HttpHeaders.CONTENT_TYPE), property.getString());
   }

   /**
    * Testing {@link WebDavServiceImpl} PUT method for correct response 
    * building. According to 'RFC-2616' it is expected to contain 'location' header.
    * More info is introduced <a href=http://tools.ietf.org/html/rfc2616#section-14.30>here</a>.
    * @throws Exception
    */
   public void testLocationHeaderInPutResponse() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();

      // now we execute the query
      ContainerResponse containerResponse =
         service(WebDAVMethods.PUT, getPathWS() + filename, "", null, content.getBytes());
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, containerResponse.getStatus());
      // check if response 'CREATED' contains 'LOCATION' header
      assertTrue(containerResponse.getHttpHeaders().containsKey(ExtHttpHeaders.LOCATION));
      // check if 'CREATED' response 'LOCATION' header contains correct location path
      assertEquals(getPathWS() + filename, containerResponse.getHttpHeaders().getFirst(ExtHttpHeaders.LOCATION)
         .toString());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
