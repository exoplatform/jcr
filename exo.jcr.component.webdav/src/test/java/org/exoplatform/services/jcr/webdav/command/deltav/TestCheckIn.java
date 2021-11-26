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
import org.exoplatform.services.jcr.webdav.utils.TestUtils;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class TestCheckIn extends BaseStandaloneTest
{

   public void testCheckIn() throws Exception
   {
      String path = TestUtils.getFileName();
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream(TestUtils.getFileContent().getBytes()),
            "nt:unstructured", "");

      Response response = new CheckInCommand().checkIn(session, path);
      assertEquals(HTTPStatus.CONFLICT, response.getStatus());

      response = new VersionControlCommand().versionControl(session, path);
      assertEquals(HTTPStatus.OK, response.getStatus());

      assertEquals(true, node.isCheckedOut());

      response = new CheckInCommand().checkIn(session, path);
      assertEquals(HTTPStatus.OK, response.getStatus());

      assertEquals(false, node.isCheckedOut());
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
