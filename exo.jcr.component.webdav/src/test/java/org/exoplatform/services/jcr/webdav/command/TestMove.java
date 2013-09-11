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
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.WebDavServiceImpl;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestMove extends BaseStandaloneTest
{
   final static private String host = "http://localhost:8080";

   public void testMoveForNonCollectionSingleWorkspace() throws Exception
   {
      testMoveForNonCollectionSingleWorkspace(getPathWS());
   }

   public void testMoveForNonCollectionSingleWorkspaceWithFakePathWS() throws Exception
   {
      testMoveForNonCollectionSingleWorkspace(getFakePathWS());
   }

   private void testMoveForNonCollectionSingleWorkspace(String pathWs) throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TestUtils.getFileName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + pathWs + destFilename);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, pathWs + filename, host, headers, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));
      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));
      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));
      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
   }

   public void testMoveForNonCollectionDiferntWorkspaces() throws Exception
   {
      assertNotSame(session.getWorkspace().getName(), destSession.getWorkspace().getName());
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TestUtils.getFileName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathDestWS() + destFilename);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(destSession.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));
      Node nodeDest = destSession.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));
      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));
      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
   }

   /**
    * Testing {@link WebDavServiceImpl} MOVE method for correct response 
    * building. According to 'RFC-2616' it is expected to contain 'location' header.
    * More info is introduced <a href=http://tools.ietf.org/html/rfc2616#section-14.30>here</a>.
    * @throws Exception
    */
   public void testLocationHeaderInMoveResponse() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TestUtils.getFileName();

      // prepare headers
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);

      // execute the query
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

      // check if 'CREATED' response contains 'LOCATION' header
      assertTrue(response.getHttpHeaders().containsKey(ExtHttpHeaders.LOCATION));
      // check if 'CREATED' response 'LOCATION' header contains correct location path
      assertEquals(host + getPathWS() + destFilename,
         response.getHttpHeaders().getFirst(ExtHttpHeaders.LOCATION).toString());
   }

   /**
    * Testing for correct destination header parsing in MOVE method.
    * We pass a path which contains escaped space - "%20" 
    * and escaped space with quote symbol "%20'"
    * @throws Exception
    */
   public void testDestinationHeaderParsing() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";

      String destFilename = TestUtils.getFileName() + " test";

      // prepare headers
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);

      // execute the query
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

      filename = destFilename;

      destFilename = TestUtils.getFileName() + " 'test";

      // prepare headers
      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);

      // execute the query
      response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

   }

   /**
    * Testing for correct response after MOVE a resource to the destination,
    * where another resource already existed
    * For more info see <a href=http://www.webdav.org/specs/rfc2518.html#METHOD_MOVE>this</a>.
    * @throws Exception
    */
   public void testNoContentResponses() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";

      String destFilename = TestUtils.getFileName();
      inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, destFilename, inputStream, defaultFileNodeType, "");

      // prepare headers
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);
      headers.add(ExtHttpHeaders.OVERWRITE, "T");

      // execute the query
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.NO_CONTENT, response.getStatus());

   }

   /**
    * Testing for correct destination header parsing using "https" 
    * instead of usual "http" scheme.
    * @throws Exception
    */
   public void testHttpsSchemeInDestinationHeaderParsing() throws Exception
   {
      String httpsHost = "https://localhost:8080";

      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";

      String destFilename = TestUtils.getFileName();

      // prepare headers
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, httpsHost + getPathWS() + destFilename);

      // execute the query
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

   }

   public void testMoveHeaderBeginsFromWorkspaceName() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      String destFilename = TestUtils.getFileName();
      filename = filename + "[2]";

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + destFilename);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
   }

   public void testMoveToFolderWithSpace() throws Exception
   {
      String folderNameWithSpace = "new folder - testMoveToFolderWithSpace";
      session.getRootNode().addNode(folderNameWithSpace, "nt:folder");
      session.save();

      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = "/" + folderNameWithSpace + TestUtils.getFileName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + destFilename);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
   }

   public void testMoveToFolderWithSpaceUnescapedChars() throws Exception
   {
      String folderNameWithSpace = "new folder - testMoveToFolderWithSpaceUnescapedChars";
      session.getRootNode().addNode(folderNameWithSpace, "nt:folder");
      session.save();

      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TextUtil.unescape("/" + folderNameWithSpace + TestUtils.getFileName(), '%');

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + destFilename);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.MOVE, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
   }

   /**
    * Here we're testing the case when we are trying to move a resource C to a path /A/B/C
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html#rfc.section.9.3.1>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testMoveToNonCollectionToNonExistingWorkspace() throws Exception
   {
      String filename = TestUtils.getFileName();
      String destFilename = TestUtils.getFileName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);

      ContainerResponse response = service(WebDAVMethods.MOVE, getPathWS() + "_" + filename, host, headers, null);

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
