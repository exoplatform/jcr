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

import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 22.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestCloneRemovedAdded.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestCloneRemovedAdded extends BaseUsecasesTest
{

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      Session session1 = (SessionImpl)repository.login(credentials, "ws");
      session1.getRootNode().addNode("my");
      session1.save(); // need save newly created node before the adding their childs
      session1.getRootNode().getNode("my").addNode("path");
      session1.save();
      session1.getRootNode().getNode("my").getNode("path").addNode("node");
      session1.save();
      session1.logout();

      // prepare the parent node for cloned
      Session session2 = (SessionImpl)repository.login(credentials, "ws2");
      session2.getRootNode().addNode("my");
      session2.save(); // need save newly created node before the adding their childs
      session2.getRootNode().getNode("my").addNode("path");
      session2.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         SessionImpl session1 = (SessionImpl)repository.login(credentials, "ws");
         session1.getRootNode().getNode("my").remove();
         session1.save();
         session1.logout();

         SessionImpl session2 = (SessionImpl)repository.login(credentials, "ws2");
         session2.getRootNode().getNode("my").remove();
         session2.save();
         session2.logout();
      }
      catch (Throwable e)
      {
         log.error("tearDown error " + e, e);
      }

      super.tearDown();
   }

   public void testClone() throws Exception
   {

      try
      {
         // clone on 2nd ws
         Session session2 = (SessionImpl)repository.login(credentials, "ws2");
         Workspace workspace1 = session2.getWorkspace();
         workspace1.clone("ws", "/my/path/node", "/my/path/node", true);
         session2.logout();

      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Clone should be successful. But error occurs " + e.getMessage());
      }
   }

   public void testCloneRemovedAdded() throws Exception
   {

      try
      {
         // delete and add /my/path/node in ws
         SessionImpl session1 = (SessionImpl)repository.login(credentials, "ws");
         session1.getRootNode().getNode("my/path/node").remove();
         session1.save();
         // add...
         session1.getRootNode().addNode("my");
         session1.save();
         session1.getRootNode().getNode("my").addNode("path");
         session1.save();
         session1.getRootNode().getNode("my").getNode("path").addNode("node");
         session1.save();
         session1.logout();

         // clone on 2nd ws
         Session session2 = (SessionImpl)repository.login(credentials, "ws2");
         Workspace workspace1 = session2.getWorkspace();
         workspace1.clone("ws", "/my/path/node", "/my/path/node", true);
         session2.logout();

      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("Clone should be successful. But error occurs " + e.getMessage());
      }
   }
}
