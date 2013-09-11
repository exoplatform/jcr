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
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestHead extends BaseStandaloneTest
{

   private String path = TestUtils.getFileName();

   private String fileContent = TestUtils.getFileContent();

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      session.getRootNode().addNode(TextUtil.relativizePath(path));
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
      path = path + "[2]";
   }

   public void testSimpleHead() throws Exception
   {
      ContainerResponse response = serviceWithEscape(WebDAVMethods.HEAD, getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());
   }

   public void testSimpleHeadWithFakePathWS() throws Exception
   {
      ContainerResponse response = serviceWithEscape(WebDAVMethods.HEAD, getFakePathWS() + path, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
