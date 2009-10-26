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

import javax.jcr.Node;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestMove extends BaseStandaloneTest
{

   public void testMoveForNonCollectionSingleWorkspace() throws Exception
   {
      String content = TestUtils.getFileContent();
      String filename = TestUtils.getFileName();
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      String destFilename = TestUtils.getFileName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, getPathWS() + destFilename);
      ContainerResponse response = service(WebDAVMethods.MOVE, getPathWS() + filename, "", headers, null);
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
      InputStream inputStream = new ByteArrayInputStream(content.getBytes());
      TestUtils.addContent(session, filename, inputStream, defaultFileNodeType, "");
      String destFilename = TestUtils.getFileName();
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add(ExtHttpHeaders.DESTINATION, getPathDestWS() + destFilename);
      ContainerResponse response = service(WebDAVMethods.MOVE, getPathWS() + filename, "", headers, null);
      assertEquals(HTTPStatus.NO_CONTENT, response.getStatus());
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

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
