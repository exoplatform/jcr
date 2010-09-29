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
package org.exoplatform.services.jcr.api.lock;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.security.AccessControlException;
import java.util.HashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestLockPermissions.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestLockPermissions extends JcrAPIBaseTest
{

   private Node lockedNode = null;

   public void setUp() throws Exception
   {

      super.setUp();

      if (lockedNode == null)
         try
         {
            lockedNode = root.addNode("rootLockPermissionsTest");
            //            if (lockedNode.canAddMixin("mix:lockable"))
            //               lockedNode.addMixin("mix:lockable");
            root.save();
         }
         catch (RepositoryException e)
         {
            fail("Child node must be accessible and readable. But error occurs: " + e);
         }
   }

   @Override
   public void tearDown() throws Exception
   {
      lockedNode.remove();
      session.save();
      super.tearDown();
   }

   public void testLockAccessDeniedException() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws");
      NodeImpl nodeToLockSession1 =
         (NodeImpl)session1.getRootNode().getNode("rootLockPermissionsTest").addNode("testLockSesssionScoped");
      nodeToLockSession1.addMixin("mix:lockable");
      nodeToLockSession1.addMixin("exo:owneable");
      nodeToLockSession1.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.READ});
      nodeToLockSession1.setPermissions(perm);
      session1.save();

      nodeToLockSession1.lock(true, false);// boolean isSessionScoped=false
      assertTrue(nodeToLockSession1.isLocked());
      nodeToLockSession1.unlock();
      assertFalse(nodeToLockSession1.isLocked());

      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.READ);
      try
      {
         session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.SET_PROPERTY);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
         //ok
      }

      Node nodeToLockSession2 =
         session2.getRootNode().getNode("rootLockPermissionsTest").getNode("testLockSesssionScoped");
      assertFalse(nodeToLockSession2.isLocked());
      try
      {
         try
         {
            // trying to lock
            nodeToLockSession2.lock(true, false);
            fail("Node locked. An AccessDeniedException should be thrown on set property but doesn't");
         }
         catch (AccessDeniedException e)
         {
            // ok
         }
      }
      finally
      {
         if (nodeToLockSession1.isLocked())
         {
            nodeToLockSession1.unlock();
         }
         session1.logout();
         session2.logout();
      }
   }

   public void testLockTimedAccessDeniedException() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws");
      NodeImpl nodeToLockSession1 =
         (NodeImpl)session1.getRootNode().getNode("rootLockPermissionsTest").addNode("testLockTimed");
      nodeToLockSession1.addMixin("mix:lockable");
      nodeToLockSession1.addMixin("exo:owneable");
      nodeToLockSession1.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.READ});
      nodeToLockSession1.setPermissions(perm);
      session1.save();

      nodeToLockSession1.lock(true, 100000);
      assertTrue(nodeToLockSession1.isLocked());
      nodeToLockSession1.unlock();
      assertFalse(nodeToLockSession1.isLocked());

      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.READ);
      try
      {
         session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.SET_PROPERTY);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
         //ok
      }

      NodeImpl nodeToLockSession2 =
         (NodeImpl)session2.getRootNode().getNode("rootLockPermissionsTest").getNode("testLockTimed");
      assertFalse(nodeToLockSession2.isLocked());
      try
      {
         try
         {
            // trying to lock
            nodeToLockSession2.lock(true, 100000);
            fail("Node locked. An AccessDeniedException should be thrown on set property but doesn't");
         }
         catch (AccessDeniedException e)
         {
            // ok
         }
      }
      finally
      {
         if (nodeToLockSession1.isLocked())
         {
            nodeToLockSession1.unlock();
         }
         session1.logout();
         session2.logout();
      }
   }

   public void testUnlockAccessDeniedException() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws");
      NodeImpl nodeToLockSession1 =
         (NodeImpl)session1.getRootNode().getNode("rootLockPermissionsTest").addNode("testUnlock");
      nodeToLockSession1.addMixin("mix:lockable");
      nodeToLockSession1.addMixin("exo:owneable");
      nodeToLockSession1.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.READ});
      nodeToLockSession1.setPermissions(perm);
      session1.save();

      Lock lock = nodeToLockSession1.lock(true, 100000);
      assertTrue(nodeToLockSession1.isLocked());

      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.READ);
      try
      {
         session2.checkPermission(nodeToLockSession1.getPath(), PermissionType.SET_PROPERTY);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
         //ok
      }

      NodeImpl nodeToLockSession2 =
         (NodeImpl)session2.getRootNode().getNode("rootLockPermissionsTest").getNode("testUnlock");
      assertTrue(nodeToLockSession2.isLocked());
      try
      {
         try
         {
            session2.addLockToken(lock.getLockToken());

            // trying to unlock
            nodeToLockSession2.unlock();
            fail("Node locked. An AccessDeniedException should be thrown on set property but doesn't");
         }
         catch (AccessDeniedException e)
         {
            // ok
         }
      }
      finally
      {
         if (nodeToLockSession1.isLocked())
         {
            nodeToLockSession1.unlock();
         }
         session1.logout();
         session2.logout();
      }
   }

}
