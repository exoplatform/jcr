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
public class TestCopy extends BaseStandaloneTest
{

   final static private String host = "http://localhost:8080";

   public void testCopyForNonCollectionSingleWorkSpace() throws Exception
   {
      testCopyForNonCollectionSingleWorkSpace(getPathWS());
   }

   public void testCopyForNonCollectionSingleWorkSpaceWithFakePathWS() throws Exception
   {
      testCopyForNonCollectionSingleWorkSpace(getFakePathWS());
   }

   private void testCopyForNonCollectionSingleWorkSpace(String pathWs) throws Exception
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
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, pathWs + filename, host, headers, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));
      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));
      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));
      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
      Node nodeBase = session.getRootNode().getNode(TextUtil.relativizePath(filename));
      assertTrue(nodeBase.hasNode("jcr:content"));
      Node nodeBaseContent = nodeBase.getNode("jcr:content");
      assertTrue(nodeBaseContent.hasProperty("jcr:data"));
      ByteArrayInputStream streamBase = (ByteArrayInputStream)nodeBaseContent.getProperty("jcr:data").getStream();
      String getContentBase = TestUtils.stream2string(streamBase, null);
      assertEquals(content, getContentBase);
   }

   public void testeCopyForNonCollectionDiferentWorkSpaces() throws Exception
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
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());

      assertTrue(destSession.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));
      Node nodeDest = destSession.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));
      Node nodeDestContent = nodeDest.getNode("jcr:content");
      nodeDestContent.hasProperty("jcr:data");
      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));
      Node nodeBase = session.getRootNode().getNode(TextUtil.relativizePath(filename));
      assertTrue(nodeBase.hasNode("jcr:content"));
      Node nodeBaseContent = nodeBase.getNode("jcr:content");
      assertTrue(nodeBaseContent.hasProperty("jcr:data"));
      ByteArrayInputStream streamBase = (ByteArrayInputStream)nodeBaseContent.getProperty("jcr:data").getStream();
      String getContentBase = TestUtils.stream2string(streamBase, null);
      assertEquals(content, getContentBase);

   }

   /**
    * Testing {@link WebDavServiceImpl} COPY method for correct response 
    * building. According to 'RFC-2616' it is expected to contain 'location' header.
    * More info is introduced <a href=http://tools.ietf.org/html/rfc2616#section-14.30>here</a>.
    * @throws Exception
    */
   public void testLocationHeaderInCopyResponse() throws Exception
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

      // execute query
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      // check if response 'CREATED' contains 'LOCATION' header
      assertTrue(response.getHttpHeaders().containsKey(ExtHttpHeaders.LOCATION));
      // check if 'CREATED' response 'LOCATION' header contains correct location path
      assertEquals(host + getPathWS() + destFilename, response.getHttpHeaders().getFirst(ExtHttpHeaders.LOCATION)
         .toString());
   }

   /**
    * Testing for correct destination header parsing in COPY method.
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
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

      filename = destFilename;

      destFilename = TestUtils.getFileName() + " 'test";

      // prepare headers
      headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, host + getPathWS() + destFilename);

      // execute the query
      response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());

   }

   /**
    * Testing for correct response after COPY a resource to the destination,
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
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
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
      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());
   }

   public void testCopyDestinationHeaderBeginsFromWorkspaceName() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TestUtils.getFileName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + destFilename);

      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));

      Node nodeBase = session.getRootNode().getNode(TextUtil.relativizePath(filename));
      assertTrue(nodeBase.hasNode("jcr:content"));

      Node nodeBaseContent = nodeBase.getNode("jcr:content");
      assertTrue(nodeBaseContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamBase = (ByteArrayInputStream)nodeBaseContent.getProperty("jcr:data").getStream();
      String getContentBase = TestUtils.stream2string(streamBase, null);
      assertEquals(content, getContentBase);
   }

   public void testCopyToFolderWithSpace() throws Exception
   {
      String folderNameWithSpace = "new folder - testCopyToFolderWithSpace";
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

      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));

      Node nodeBase = session.getRootNode().getNode(TextUtil.relativizePath(filename));
      assertTrue(nodeBase.hasNode("jcr:content"));

      Node nodeBaseContent = nodeBase.getNode("jcr:content");
      assertTrue(nodeBaseContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamBase = (ByteArrayInputStream)nodeBaseContent.getProperty("jcr:data").getStream();
      String getContentBase = TestUtils.stream2string(streamBase, null);
      assertEquals(content, getContentBase);
   }

   public void testCopyToFolderWithSpaceUnescapedChars() throws Exception
   {
      String folderNameWithSpace = "new folder - testCopyToFolderWithSpaceUnescapedChars";
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

      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CREATED, response.getStatus());
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(destFilename)));

      Node nodeDest = session.getRootNode().getNode(TextUtil.relativizePath(destFilename));
      assertTrue(nodeDest.hasNode("jcr:content"));

      Node nodeDestContent = nodeDest.getNode("jcr:content");
      assertTrue(nodeDestContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamDest = (ByteArrayInputStream)nodeDestContent.getProperty("jcr:data").getStream();
      String getContentDest = TestUtils.stream2string(streamDest, null);
      assertEquals(content, getContentDest);
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(filename)));

      Node nodeBase = session.getRootNode().getNode(TextUtil.relativizePath(filename));
      assertTrue(nodeBase.hasNode("jcr:content"));

      Node nodeBaseContent = nodeBase.getNode("jcr:content");
      assertTrue(nodeBaseContent.hasProperty("jcr:data"));

      ByteArrayInputStream streamBase = (ByteArrayInputStream)nodeBaseContent.getProperty("jcr:data").getStream();
      String getContentBase = TestUtils.stream2string(streamBase, null);
      assertEquals(content, getContentBase);
   }

   /**
    * Here we're testing the case when we are trying to copy a resource C to a path /A/B/C
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html#rfc.section.9.8.5>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testCopyResourceToNonExistingWorkspace() throws Exception
   {
      String folderName = "new folder";
      session.getRootNode().addNode(folderName, "nt:folder");
      session.save();

      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      session.getRootNode().addNode(TextUtil.relativizePath(filename));
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      filename = filename + "[2]";
      String destFilename = TextUtil.unescape("/" + folderName + TestUtils.getFileName(), '%');

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      // add destination header with incorrect data
      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + "_" + destFilename);

      ContainerResponse response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());

      // add overwrite header to check the behavior
      headers.add(ExtHttpHeaders.OVERWRITE, "T");
      response = serviceWithEscape(WebDAVMethods.COPY, getPathWS() + filename, host, headers, null);

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());

      // clean up
      session.getRootNode().getNode(folderName).remove();
   }

   /**
    * Here we're testing the case when we are trying to copy a collection B to a path /A/B
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html#rfc.section.9.8.5>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testCopyCollectionToNonExistingWorkspace() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      // add destination header with incorrect data
      headers.add(ExtHttpHeaders.DESTINATION, WORKSPACE + "_" + "/" + "test");

      ContainerResponse response =
         service(WebDAVMethods.COPY, getPathWS() + TestUtils.getFolderName(), host, headers, null);

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());

      // add overwrite header to check the behavior
      headers.add(ExtHttpHeaders.OVERWRITE, "T");
      response = service(WebDAVMethods.COPY, getPathWS() + TestUtils.getFolderName(), host, headers, null);

      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
