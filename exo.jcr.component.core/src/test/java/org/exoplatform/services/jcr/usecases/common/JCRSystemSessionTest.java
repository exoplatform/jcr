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

package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class JCRSystemSessionTest extends BaseUsecasesTest
{
   public void testActionsOnJcrSystem() throws Exception
   {
      String workspaceName = repository.getSystemWorkspaceName();
      // ---------------use System sestion
      Session session2 = repository.getSystemSession(workspaceName);
      // ----------- Use addmin session
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspaceName);
      Node node1 = session.getRootNode().addNode("Node1");
      session.save();
      // refresh session2 (session2 is systemSession )
      // session2.refresh(true) ;
      assertNotNull(session.getRootNode().getNode("Node1"));
      assertNotNull(session2.getRootNode().getNode("Node1"));
   }
}
