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
package org.exoplatform.services.jcr.impl.access;

import java.security.AccessControlException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

/**
 * Created by The eXo Platform SAS. <br/>Date: 16.10.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex
 *         Reshetnyak</a>
 * @version $Id: TestAutoInitPermissions.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestAutoInitPermissions
   extends BaseStandaloneTest
{

   @Override
   protected String getRepositoryName()
   {
      return "db1";
   }

   public void testCheckAutoInitPermissions() throws Exception
   {
      AccessControlList adminRootACL = ((NodeImpl) root).getACL();
      if (log.isDebugEnabled())
         log.debug(adminRootACL.dump());

      assertTrue(root.hasProperties());

      try
      {
         Session johnSession =
                  repository.login(new CredentialsImpl("john", "exo".toCharArray()), session.getWorkspace().getName());
         NodeImpl myNode = (NodeImpl) johnSession.getRootNode().addNode("node_for_john");
         johnSession.save();

         Node test = myNode.addNode("test");
         test.setProperty("property", "any data");
         myNode.save();
         test.remove();
         myNode.save();

         AccessControlList johnRootACL = ((NodeImpl) johnSession.getRootNode()).getACL();
         if (log.isDebugEnabled())
            log.debug(johnRootACL.dump());

         assertTrue(johnSession.getRootNode().hasProperties());

      }
      catch (AccessControlException e)
      {
         e.printStackTrace();
         fail(e.getMessage());
      }
   }

}
