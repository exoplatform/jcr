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

import javax.jcr.Node;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestMkCol extends BaseStandaloneTest
{

   public void testSimpleMkCol() throws Exception
   {
      String folder = TestUtils.getFolderName();
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + folder, "", null, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
   }

   public void testMkCol() throws Exception
   {
      String folder = TestUtils.getFolderName();
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + folder, "", null, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      String file = TestUtils.getFileName();
      String path = folder + file;
      String content = TestUtils.getFileContent();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
      assertTrue(session.getRootNode().hasNode(TextUtil.relativizePath(folder)));
      Node folderNode = session.getRootNode().getNode(TextUtil.relativizePath(folder));
      assertTrue(folderNode.hasNode(TextUtil.relativizePath(file)));
   }

   public void testFolderNodeTypeHeader() throws Exception
   {
      String folder = TestUtils.getFolderName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      headers.add(ExtHttpHeaders.FOLDER_NODETYPE, "nt:file");
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + folder, "", headers, null);
      assertEquals(HTTPStatus.UNSUPPORTED_TYPE, response.getStatus());

      headers.clear();
      headers.add(ExtHttpHeaders.FILE_NODETYPE, "nt:folder");
      response = service(WebDAVMethods.MKCOL, getPathWS() + folder, "", headers, null);
      assertEquals(HTTPStatus.CREATED, response.getStatus());
   }

   /**
    * Testing {@link WebDavServiceImpl} MKCOL method for correct response 
    * building. According to 'RFC-2616' it is expected to contain 'location' header.
    * More info is introduced <a href=http://tools.ietf.org/html/rfc2616#section-14.30>here</a>.
    * @throws Exception
    */
   public void testLocationHeaderInMkColResponse() throws Exception
   {
      String folder = TestUtils.getFolderName();

      // execute query
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + folder, "", null, null);
      // check if operation completed successfully, we expect a new resource to be created
      assertEquals(HTTPStatus.CREATED, response.getStatus());
      // here we check if response 'CREATED' contains 'LOCATION' header
      assertTrue(response.getHttpHeaders().containsKey(ExtHttpHeaders.LOCATION));
      // here we check if 'CREATED' response 'LOCATION' header contains correct location path
      assertEquals(getPathWS() + folder, response.getHttpHeaders().getFirst(ExtHttpHeaders.LOCATION).toString());

   }

   /**
    * Here we're testing the case when we are trying to create a collection B at a path /A/B
    * and a A collection does not exist. According to the <a href=http://www.webdav.org/specs/rfc4918.html#rfc.section.9.3.1>
    * RFC 4918</a> section we are to receive 409(conflict) HTTP status. 
    * @throws Exception
    */
   public void testMkColInNonExistingWorkspace() throws Exception
   {
      String folder = TestUtils.getFolderName();
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + "_" + folder, "", null, null);
      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   public void testConflict() throws Exception
   {
      String folder = TestUtils.getFolderName();
      ContainerResponse response = service(WebDAVMethods.MKCOL, getPathWS() + folder + folder, "", null, null);
      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }
}
