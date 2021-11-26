/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav.command.deltav;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
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

public class TestVersionControl extends BaseStandaloneTest
{

   public void testVersionControl() throws Exception
   {
      testVersionControl(getPathWS());
   }

   public void testVersionControlWithFakePathWS() throws Exception
   {
      testVersionControl(getFakePathWS());
   }

   private void testVersionControl(String pathWs) throws Exception
   {
      String path = TestUtils.getFileName();
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream(TestUtils.getFileContent().getBytes()),
            defaultFileNodeType, MediaType.TEXT_PLAIN);
      assertEquals(false, node.isNodeType("mix:versionable"));
      ContainerResponse response = service("VERSION-CONTROL", pathWs + path, "", null, null);
      assertEquals(HTTPStatus.OK, response.getStatus());
      Node verNode = session.getRootNode().getNode(TextUtil.relativizePath(path));
      assertEquals(true, verNode.isNodeType("mix:versionable"));
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
