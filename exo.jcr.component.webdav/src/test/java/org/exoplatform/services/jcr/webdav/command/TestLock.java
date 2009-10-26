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
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 10 Dec 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: TestLock.java
 */

public class TestLock extends BaseStandaloneTest
{

   private String path = TestUtils.getFileName();

   private String fileContent = TestUtils.getFileContent();

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
      TestUtils.addContent(session, path, inputStream, defaultFileNodeType, "");
   }

   public void testLock() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.add("Content-Type", MediaType.TEXT_PLAIN);
      ContainerResponse containerResponse = service(WebDAVMethods.LOCK, getPathWS() + path, "", headers, null);
      assertEquals(HTTPStatus.OK, containerResponse.getStatus());
      containerResponse = service("DELETE", getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.LOCKED, containerResponse.getStatus());
      assertTrue(session.getRootNode().getNode(TextUtil.relativizePath(path)).isLocked());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
