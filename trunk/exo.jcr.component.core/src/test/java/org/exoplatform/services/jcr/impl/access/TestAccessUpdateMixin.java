/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.access;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestAccessUpdateMixin.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestAccessUpdateMixin extends BaseStandaloneTest
{

   @Override
   public String getRepositoryName()
   {
      return "db1";
   }

   public void setUp() throws Exception
   {
      super.setUp();
      //create nodes with "john" user
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Node testRoot = sessJohn.getRootNode().addNode("testRoot");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setProperty("prop", "value");
      sessJohn.save();
      sessJohn.logout();
   }

   public void tearDown() throws Exception
   {
      Session sysSession = this.repository.getSystemSession(session.getWorkspace().getName());
      if (sysSession.getRootNode().hasNode("testRoot"))
      {
         Node testRoot = sysSession.getRootNode().getNode("testRoot");
         testRoot.remove();
         sysSession.save();
      }
      super.tearDown();
   }

   public void testUpdateWhenParentHasRightsButChildNot() throws Exception
   {
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      NodeImpl subNode = (NodeImpl)sessJohn.getRootNode().getNode("testRoot").addNode("testNode");
      subNode.addMixin("exo:privilegeable");
      sessJohn.save();

      NodeImpl testRoot = (NodeImpl)sessJohn.getRootNode().getNode("testRoot");

      testRoot.setPermission("mary", PermissionType.ALL);
      testRoot.setPermission("john", PermissionType.ALL);
      testRoot.removePermission(SystemIdentity.ANY);

      subNode.setPermission("mary", new String[]{PermissionType.READ, PermissionType.SET_PROPERTY});
      subNode.removePermission(SystemIdentity.ANY);
      sessJohn.save();
      sessJohn.logout();

      // login as Mary with no rights, and try to addmixin
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      subNode = (NodeImpl)sessMary.getRootNode().getNode("testRoot").getNode("testNode");

      try
      {
         subNode.addMixin("mix:referenceable");
         sessMary.save();
         fail();
      }
      catch (AccessDeniedException e)
      {
         //ok
      }
      finally
      {
         sessMary.logout();
      }
   }

   public void testUpdateWhenChildHasRightsButParentNot() throws Exception
   {
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      NodeImpl subNode = (NodeImpl)sessJohn.getRootNode().getNode("testRoot").addNode("testNode");
      subNode.addMixin("exo:privilegeable");
      sessJohn.save();

      NodeImpl testRoot = (NodeImpl)sessJohn.getRootNode().getNode("testRoot");

      testRoot.setPermission("mary", new String[]{PermissionType.READ});
      testRoot.setPermission("john", PermissionType.ALL);
      testRoot.removePermission(SystemIdentity.ANY);

      subNode.setPermission("mary", PermissionType.ALL);
      subNode.removePermission(SystemIdentity.ANY);
      sessJohn.save();
      sessJohn.logout();

      // login as Mary with no rights, and try to addmixin
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      subNode = (NodeImpl)sessMary.getRootNode().getNode("testRoot").getNode("testNode");

      try
      {
         subNode.addMixin("mix:referenceable");
         sessMary.save();
      }
      catch (AccessDeniedException e)
      {
         fail("There must not be access denied exception.");
      }
      finally
      {
         sessMary.logout();
      }
   }

}