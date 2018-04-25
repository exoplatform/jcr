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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.infinispan.IllegalLifecycleStateException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestLockImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestLockImpl extends JcrImplBaseTest
{
   private ExtendedNode lockedNode = null;

   private WorkspaceLockManager service;

   private static final long LOCK_TIMEOUT = 5; // sec

   private static final long LOCK_REMOVER_WAIT = LockRemover.DEFAULT_THREAD_TIMEOUT + (LOCK_TIMEOUT + 1) * 1000; // 15

   // sec

   public void setUp() throws Exception
   {

      super.setUp();

      service = (WorkspaceLockManager)container.getComponentInstanceOfType(WorkspaceLockManager.class);

      if (lockedNode == null)
         try
         {
            lockedNode = (ExtendedNode)root.addNode("locked node");
            if (lockedNode.canAddMixin("mix:lockable"))
               lockedNode.addMixin("mix:lockable");
            root.save();
         }
         catch (RepositoryException e)
         {
            fail("Child node must be accessible and readable. But error occurs: " + e);
         }
   }

   public void testNonSessionScopedLockRemoveOnTimeOut()
   {
      try
      {
         LockImpl lock = (LockImpl)lockedNode.lock(true, false);

         assertTrue(lockedNode.isLocked());
         lock.setTimeOut(LOCK_TIMEOUT);// 5 sec
         if (log.isDebugEnabled())
            log.debug("Stoping thread. Wait for removing lock for node "
               + ((NodeImpl)lockedNode).getData().getIdentifier() + "by LockRemover");
         Thread.sleep(LOCK_REMOVER_WAIT);
         assertFalse(lockedNode.isLocked());

      }
      catch (RepositoryException e)
      {
         fail(e.getLocalizedMessage());
      }
      catch (InterruptedException e)
      {
         fail(e.getLocalizedMessage());
      }
   }

   public void testSessionScopedLockRemoveOnTimeOut()
   {
      try
      {
         LockImpl lock = (LockImpl)lockedNode.lock(true, true);
         assertTrue(lockedNode.isLocked());
         lock.setTimeOut(LOCK_TIMEOUT); // sec
         if (log.isDebugEnabled())
            log.debug("Stoping thread. Wait for removing lock by LockRemover");
         Thread.sleep(LOCK_REMOVER_WAIT);
         assertTrue(lockedNode.isLocked());
         lockedNode.unlock();
      }
      catch (RepositoryException e)
      {
         fail(e.getLocalizedMessage());
      }
      catch (InterruptedException e)
      {
         fail(e.getLocalizedMessage());
      }
   }

   public void testRemoveLockProperties() throws Exception
   {
      Node node = session.getRootNode().addNode("testLock");
      node.addMixin("mix:lockable");
      session.save();

      node.lock(false, false);

      assertTrue(node.isLocked());

      // remove lock properties from JCR tables
      JDBCWorkspaceDataContainer container =
         (JDBCWorkspaceDataContainer)repository.getWorkspaceContainer("ws").getComponent(
            JDBCWorkspaceDataContainer.class);
      CacheableWorkspaceDataManager dataManager =
         (CacheableWorkspaceDataManager)repository.getWorkspaceContainer("ws").getComponent(
            CacheableWorkspaceDataManager.class);

      System.setProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE, "true");
      try
      {
         // Remove info from the db
         container.start();
         //wait remove thread lock finished (asynchrone remove)
         Thread.sleep(LOCK_REMOVER_WAIT);
         // Remove info from the cache
         dataManager.start();
      }
      finally
      {
         System.setProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE, "false");
      }

      // node still locked, because there is lock data in lock tables
      assertTrue(node.isLocked());

      try
      {
         node.unlock();
         fail("Exception should be thrown");
      }
      catch (Exception e)
      {
      }

      // remove locks from lock table
      AbstractCacheableLockManager lockManager =
         (AbstractCacheableLockManager)repository.getWorkspaceContainer("ws").getComponent(
            AbstractCacheableLockManager.class);

      System.setProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE, "true");
      try
      {
         lockManager.start();
      }
      finally
      {
         System.setProperty(AbstractCacheableLockManager.LOCKS_FORCE_REMOVE, "false");
      }

      // node should not be locked after removing lock data from lock tables
      assertFalse(node.isLocked());
   }

   public void testLoadLocksAfterStopStart() throws Exception
   {
      Node node = session.getRootNode().addNode("testLock");
      node.addMixin("mix:lockable");
      node.addMixin("mix:referenceable");
      session.save();

      node.lock(false, false);
      session.save();

      assertTrue(node.isLocked());

      AbstractCacheableLockManager lockManager =
         (AbstractCacheableLockManager)repository.getWorkspaceContainer("ws").getComponent(
            AbstractCacheableLockManager.class);
      
      lockManager.stop();

      try
      {
         assertFalse(lockManager.lockExist(node.getUUID()));
      }
      catch (IllegalLifecycleStateException e)
      {
         // not check for ISPN cache.
      }
      
      lockManager.start();
      
      assertTrue(lockManager.lockExist(node.getUUID()));
   }

}
