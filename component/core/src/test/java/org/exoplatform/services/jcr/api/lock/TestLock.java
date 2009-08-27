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
package org.exoplatform.services.jcr.api.lock;

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 21.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestLock.java 11908 2008-03-13 16:00:12Z ksm $
 */
public class TestLock
   extends JcrAPIBaseTest
{

   private Node lockedNode = null;

   public void setUp() throws Exception
   {

      super.setUp();

      if (lockedNode == null)
         try
         {
            lockedNode = root.addNode("locked node");
            if (lockedNode.canAddMixin("mix:lockable"))
               lockedNode.addMixin("mix:lockable");
            root.save();
         }
         catch (RepositoryException e)
         {
            fail("Child node must be accessible and readable. But error occurs: " + e);
         }
   }

   /**
    * Check 1. if locked node can't be changed. 2. if session with lock tocked able to change locked
    * property
    * 
    * @throws RepositoryException
    */
   public void testLock() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws");
      Node nodeToLockSession1 = session1.getRootNode().addNode("testLockSesssionScoped");
      nodeToLockSession1.addMixin("mix:lockable");
      session1.save();
      Lock lock = nodeToLockSession1.lock(true, false);// boolean isSessionScoped=false
      assertTrue(nodeToLockSession1.isLocked());
      nodeToLockSession1.setProperty("property #1", "1");
      session1.save();

      // try change property from another session
      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      Node nodeToLockSession2 = session2.getRootNode().getNode("testLockSesssionScoped");
      assertEquals(true, nodeToLockSession2.isLocked());
      try
      {
         try
         {
            // trying...
            nodeToLockSession2.setProperty("property #1", "2");
            fail("Node locked. An exception should be thrown on set property but doesn't");
         }
         catch (LockException e)
         {
            // ok
         }

         // add lock tocken try again
         session2.addLockToken(lock.getLockToken());
         try
         {
            nodeToLockSession2.setProperty("property #1", "2");
            // ok
         }
         catch (LockException e)
         {
            e.printStackTrace();
            fail("Session has lock tocken. But an exception was thrown on set property. " + e);
         }
         // lock
      }
      finally
      {
         nodeToLockSession1.unlock();
         session1.logout();
      }
   }

   /**
    * Check if session scoped lock 1. Will disallow another session to change the node till the lock
    * session live 2. Will allow the lock after the session will be logouted
    * 
    * @throws RepositoryException
    */
   public void testLockSesssionScoped() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()), "ws");
      Node nodeToLockSession1 = session1.getRootNode().addNode("testLockSesssionScoped");
      nodeToLockSession1.addMixin("mix:lockable");
      session1.save();
      nodeToLockSession1.lock(true, true);// boolean isSessionScoped=true
      assertTrue(nodeToLockSession1.isLocked());
      nodeToLockSession1.setProperty("property #1", "1");
      session1.save();

      // try change property from another session
      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      Node nodeToLockSession2 = session2.getRootNode().getNode("testLockSesssionScoped");
      assertEquals(true, nodeToLockSession2.isLocked());
      try
      {
         // trying...
         nodeToLockSession2.setProperty("property #1", "2");
         fail("Node locked. An exception should be thrown on set property but doesn't");
      }
      catch (LockException e)
      {
         // ok
      }
      finally
      {
         session1.logout();
      }

      try
      {
         // trying again...
         // session was logouted and session scoped lock was released
         nodeToLockSession2.setProperty("property #1", "2");
         // ok
      }
      catch (LockException e)
      {
         fail("There no lock should found. But an exception was thrown on set property. " + e);
      }
      finally
      {
         session2.logout();
      }
   }

   public void testLockByOwner() throws RepositoryException
   {

      try
      {
         lockedNode.lock(true, true);
         Node foo = lockedNode.addNode("foo");
         foo.addNode("bar"); // throws LockException "Node /node/foo is locked"
         lockedNode.save();
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         lockedNode.unlock();
         lockedNode.addNode("foo");
         session.save();
         lockedNode.lock(true, true);
         lockedNode.getNode("foo").addNode("bar"); // throws LockException "Node
         // /node/foo is locked"
         lockedNode.save();
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }

      try
      {
         lockedNode.unlock();
         lockedNode.addNode("foo");
         session.save();
         lockedNode.lock(true, true);
         lockedNode.getNode("foo").setProperty("bar", "bar"); // throws LockException
         // "Node /node/foo is locked"
         lockedNode.save();
         lockedNode.unlock();
      }
      catch (RepositoryException e)
      {
         fail("Child node must be accessible and readable. But error occurs: " + e);
      }
   }

   public void testLockByOwnerAnotherSession() throws RepositoryException
   {
      Session session1 = repository.login(new CredentialsImpl("admin", "admin".toCharArray()), "ws");
      Node nodeToLockSession1 = session1.getRootNode().addNode("nodeToLockSession1");
      if (nodeToLockSession1.canAddMixin("mix:lockable"))
         nodeToLockSession1.addMixin("mix:lockable");
      session1.save();
      Lock lock = nodeToLockSession1.lock(true, false);// boolean isSessionScoped
      // in ECM we are using lock(true, true) without saving lockToken
      assertTrue(nodeToLockSession1.isLocked());
      String lockToken = lock.getLockToken();
      session1.logout();
      //
      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      Node nodeToLockSession2 = session2.getRootNode().getNode("nodeToLockSession1");
      assertEquals(true, nodeToLockSession2.isLocked());
      session2.addLockToken(lockToken);
      // make sure you made this operation, otherwise you can't do unlock
      try
      {
         nodeToLockSession2.unlock();
         assertFalse(nodeToLockSession2.isLocked());
      }
      catch (Exception e)
      {
         fail("unlock() method should pass ok, as admin is lockOwner, but error occurs: " + e);
      }
   }

   public void testCreateAfterLockWithFile() throws RepositoryException
   {
      String lockToken = "";
      String nodeName = "nodeToLockAndDelete" + System.currentTimeMillis();
      //
      try
      {
         {
            Session localSession = repository.login(new CredentialsImpl("admin", "admin".toCharArray()), "ws");

            Node folder1 = localSession.getRootNode().addNode(nodeName, "nt:folder");
            localSession.save();

            Node file1 = folder1.addNode(nodeName, "nt:file");

            Node resourceNode = file1.addNode("jcr:content", "nt:resource");
            resourceNode.setProperty("jcr:mimeType", "text/xml");
            resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
            resourceNode.setProperty("jcr:data", new ByteArrayInputStream("VETAL_OK".getBytes()));

            localSession.save();

            file1.addMixin("mix:lockable");
            localSession.save();

            Lock lock = file1.lock(true, false);
            assertTrue(file1.isLocked());

            lockToken = lock.getLockToken();
            localSession.logout();
         }

         {
            Session localSession = repository.login(new CredentialsImpl("admin", "admin".toCharArray()), "ws");
            Node folder1 = localSession.getRootNode().getNode(nodeName);
            Node file1 = folder1.getNode(nodeName);
            assertTrue(file1.isLocked());
            file1.remove();
            localSession.save();
            localSession.logout();
         }

         {
            Session localSession = repository.login(new CredentialsImpl("admin", "admin".toCharArray()), "ws");

            Node folder1 = localSession.getRootNode().getNode(nodeName);

            Node file1 = folder1.addNode(nodeName, "nt:file");

            Node resourceNode = file1.addNode("jcr:content", "nt:resource");
            resourceNode.setProperty("jcr:mimeType", "text/xml");
            resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
            resourceNode.setProperty("jcr:data", new ByteArrayInputStream("VETAL_OK".getBytes()));

            localSession.save();
            localSession.logout();
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
         fail("error while adding same name node: " + e);
      }
   }

   public void testCopyLockedNode() throws Exception
   {
      Session session1 = repository.login(new CredentialsImpl("admin", "admin".toCharArray()), "ws");
      Node nodeToCopyLock = session1.getRootNode().addNode("node2testCopyLockedNode");
      if (nodeToCopyLock.canAddMixin("mix:lockable"))
         nodeToCopyLock.addMixin("mix:lockable");
      session1.save();
      Lock lock = nodeToCopyLock.lock(true, false);// boolean isSessionScoped
      // in ECM we are using lock(true, true) without saving lockToken
      assertTrue(nodeToCopyLock.isLocked());

      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");

      Node lockedNode = session2.getRootNode().getNode("node2testCopyLockedNode");

      assertTrue(nodeToCopyLock.isLocked());

      Node destParent = session2.getRootNode().addNode("destParent");
      session2.save();
      session2.getWorkspace().copy(lockedNode.getPath(), destParent.getPath() + "/" + lockedNode.getName());
      Node destCopyNode = destParent.getNode("node2testCopyLockedNode");

      assertFalse(destCopyNode.isLocked());
      try
      {
         destCopyNode.lock(true, true);
      }
      catch (RepositoryException e)
      {
         fail("to lock node");
      }
      assertTrue(destCopyNode.isLocked());

      destCopyNode.unlock();
      nodeToCopyLock.unlock();
   }

   public void testRemoveMixLockable()
   {
      try
      {
         lockedNode.removeMixin("mix:lockable");
         root.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("removeMixin(\"mix:lockable\") impossible due to error " + e.getMessage());
      }
   }

   public void testRemoveMixLockableLocked() throws Exception
   {

      lockedNode.lock(true, false);

      try
      {
         lockedNode.removeMixin("mix:lockable");
         root.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("removeMixin(\"mix:lockable\") impossible due to error " + e.getMessage());
      }
   }

   public void testRemoveMixLockableLockedWithoutToken() throws Exception
   {

      lockedNode.lock(true, false);

      try
      {
         Session s1 =
                  repository.login(new CredentialsImpl("exo", "exo".toCharArray()), session.getWorkspace().getName());
         s1.getRootNode().getNode("locked node").removeMixin("mix:lockable");
         s1.save();

         fail("removeMixin(\"mix:lockable\") should throw LockException if use hasn't lock token");
      }
      catch (LockException e)
      {
         // ok
      }
   }

   public void testLockChild() throws Exception
   {
      Node childLockNode = lockedNode.addNode("childLock");
      childLockNode.addMixin("mix:lockable");
      session.save();
      assertFalse(childLockNode.isLocked());
      childLockNode.lock(false, true);
      assertTrue(childLockNode.isLocked());
      session.save();

      Session s1 = repository.login(new CredentialsImpl("exo", "exo".toCharArray()), session.getWorkspace().getName());
      Node lock2Node = s1.getRootNode().getNode("locked node");
      assertFalse(lock2Node.isLocked());
      lock2Node.lock(false, true);
      s1.save();
      assertTrue(lock2Node.isLocked());

      try
      {
         childLockNode.remove();
      }
      catch (LockException e)
      {
         // ok
      }
      childLockNode.unlock();
      session.save();
      assertFalse(childLockNode.isLocked());

   }
}
