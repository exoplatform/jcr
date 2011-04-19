/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.common;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: DynamicSessionProviderTest.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class DynamicSessionProviderTest
   extends BaseStandaloneTest
{
   private NodeImpl testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = (NodeImpl)root.addNode("testDynamicSession");
      root.save();
   }
   
   public void testDynamicSession() throws Exception
   {
      // Mary only node, Mary membership is '*:/platform/users', seems it's user
      NodeImpl maryNode = (NodeImpl) testRoot.addNode("mary_dynamic");
      maryNode.addMixin("exo:privilegeable");
      if (!session.getUserID().equals("mary"))
      {
         maryNode.setPermission("*:/platform/users", new String[] {PermissionType.READ});
         maryNode.setPermission("mary", PermissionType.ALL);
         maryNode.removePermission(session.getUserID());
      }
      maryNode.removePermission(SystemIdentity.ANY);
      testRoot.save();

      Session marySession =
                  repository.login(new CredentialsImpl("mary", "exo".toCharArray()), session.getWorkspace().getName());
      NodeImpl myNode = (NodeImpl) marySession.getItem(maryNode.getPath());
      NodeImpl test = (NodeImpl) myNode.addNode("test");
      test.setProperty("property", "any data");
      myNode.save();
      marySession.logout();

      //Dynamic session fail read
      List<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
      accessControlEntries.add(new AccessControlEntry("*:/platform/administrators", "READ"));
      SessionProvider dynamicProvider = SessionProvider.createProvider(accessControlEntries);

      Session dynamicSession = null;
      try
      {
         dynamicSession = dynamicProvider.getSession(session.getWorkspace().getName(), repository);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());
         fail("Dynamic session with membership '*:/platform/users' should not read node with membership '*:/platform/users'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }

      //Dynamic session successful read
      accessControlEntries = new ArrayList<AccessControlEntry>();
      accessControlEntries.add(new AccessControlEntry("*:/platform/users", "READ"));
      dynamicProvider = SessionProvider.createProvider(accessControlEntries);

      //check get
      try
      {
         dynamicSession = dynamicProvider.getSession(session.getWorkspace().getName(), repository);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());
         //ok
      }
      catch (AccessDeniedException e)
      {

         e.printStackTrace();
         fail("Dynamic session with membership '*:/platform/users' should read node with membership '*:/platform/users'. Exception message :"
                  + e.getMessage());
      }

      //check add
      try
      {
         dynamicSession = dynamicProvider.getSession(session.getWorkspace().getName(), repository);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());

         maryNodeDynamic.addNode("test2");
         maryNodeDynamic.save();
         fail("Dynamic session with membership '*:/platform/users' should be not add child node with membership '*:/platform/users READ'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }

      //check remove
      try
      {
         dynamicSession = dynamicProvider.getSession(session.getWorkspace().getName(), repository);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());

         maryNodeDynamic.getNode("test").remove();
         maryNodeDynamic.save();
         fail("Dynamic session with membership '*:/platform/users' should be not remove child node with membership '*:/platform/users READ'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }
   }
}
