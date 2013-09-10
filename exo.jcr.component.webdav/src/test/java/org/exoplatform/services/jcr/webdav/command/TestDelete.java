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
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */

public class TestDelete extends BaseStandaloneTest
{

   public void testDeleteForNonCollection() throws Exception
   {
      String path = TestUtils.getFileName();
      String fileContent = TestUtils.getFileContent();
      session.getRootNode().addNode(TextUtil.relativizePath(path));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
      path = path + "[2]";
      ContainerResponse response = serviceWithEscape(WebDAVMethods.DELETE, getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.NO_CONTENT, response.getStatus());
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(path)));
   }

   public void testDeleteForCollection() throws Exception
   {
      String path = TestUtils.getFileName();
      String fileContent = TestUtils.getFileContent();
      String folderName = TestUtils.getFolderName();
      session.getRootNode().addNode(TextUtil.relativizePath(folderName));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addFolder(session, folderName, defaultFolderNodeType, "");
      TestUtils.addContent(session, folderName + path, inputStream, defaultFileNodeType, "");
      folderName = folderName + "[2]";
      ContainerResponse response = serviceWithEscape(WebDAVMethods.DELETE, getPathWS() + folderName, "", null, null);
      assertEquals(HTTPStatus.NO_CONTENT, response.getStatus());
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(folderName)));
   }

   public void testDeleteWithLock() throws Exception
   {
      String path = TestUtils.getFileName();
      String fileContent = TestUtils.getFileContent();
      session.getRootNode().addNode(TextUtil.relativizePath(path));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
      path = path + "[2]";
      String lockToken = TestUtils.lockNode(session, path, true);
      ContainerResponse response = serviceWithEscape(WebDAVMethods.DELETE, getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.LOCKED, response.getStatus());
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.LOCKTOKEN, lockToken);
      response = serviceWithEscape(WebDAVMethods.DELETE, getPathWS() + path, "", headers, null);
      assertEquals(HTTPStatus.NO_CONTENT, response.getStatus());
      assertFalse(session.getRootNode().hasNode(TextUtil.relativizePath(path)));
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
