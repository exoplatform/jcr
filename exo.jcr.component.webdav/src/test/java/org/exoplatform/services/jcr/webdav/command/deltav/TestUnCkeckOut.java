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
package org.exoplatform.services.jcr.webdav.command.deltav;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.ws.rs.core.MediaType;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class TestUnCkeckOut extends BaseStandaloneTest
{

   public void testUnCkeckOut() throws Exception
   {
      String path = TestUtils.getFileName();
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream(TestUtils.getFileContent().getBytes()),
            defaultFileNodeType, MediaType.TEXT_PLAIN);
      ContainerResponse response = service("UNCHECKOUT", getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.CONFLICT, response.getStatus());
      if (!node.isNodeType("mix:versionable"))
      {
         node.addMixin("mix:versionable");
         session.save();
      }
      response = service("UNCHECKOUT", getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.INTERNAL_ERROR, response.getStatus());
      node.checkin();
      node.checkout();
      response = service("UNCHECKOUT", getPathWS() + path, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
