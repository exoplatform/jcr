/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.lock.infinispan;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.LockData;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * The locks a node sees: a lock written in its memory and not yet in the store, as a node holds the lock another node
 * takes while that node has not written the shared store yet.
 */
public class TestISPNCacheableLockManagerImpl extends JcrImplBaseTest
{
   private final TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   /**
    * A repository of its own, whose lock store holds no lock when its workspace starts.
    */
   private RepositoryImpl lockRepository;

   private WorkspaceEntry workspaceEntry;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      lockRepository = (RepositoryImpl)helper.createRepository(container, DatabaseStructureType.SINGLE, null);
      workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);
      helper.addWorkspace(lockRepository, workspaceEntry);
   }

   @Override
   protected void tearDown() throws Exception
   {
      helper.removeRepository(container, lockRepository.getName());
      super.tearDown();
   }

   public void testLockInMemoryAndNotYetInTheStoreIsSeen() throws Exception
   {
      Session session = lockRepository.login(credentials, workspaceEntry.getName());
      try
      {
         Node node = session.getRootNode().addNode("locked");
         node.addMixin("mix:lockable");
         session.save();
         String nodeId = ((NodeImpl)node).getIdentifier();
         ISPNCacheableLockManagerImpl lockManager = lockManager();
         assertEquals(0, lockManager.getNumLocks());

         lockStore(lockManager).withFlags(Flag.SKIP_CACHE_STORE)
                               .put(nodeId, new LockData(nodeId, "token", false, false, "another", 3600000L));
         try
         {
            assertEquals(1, lockManager.getNumLocks());
            assertTrue(session.getRootNode().getNode("locked").isLocked());
         }
         finally
         {
            lockStore(lockManager).withFlags(Flag.SKIP_CACHE_STORE).remove(nodeId);
         }
         assertEquals(0, lockManager.getNumLocks());
         assertFalse(session.getRootNode().getNode("locked").isLocked());
      }
      finally
      {
         session.logout();
      }
   }

   private ISPNCacheableLockManagerImpl lockManager()
   {
      return (ISPNCacheableLockManagerImpl)lockRepository.getWorkspaceContainer(workspaceEntry.getName())
                                                    .getComponent(WorkspaceLockManager.class);
   }

   @SuppressWarnings("unchecked")
   private static AdvancedCache<Serializable, Object> lockStore(ISPNCacheableLockManagerImpl lockManager) throws Exception
   {
      Field cache = ISPNCacheableLockManagerImpl.class.getDeclaredField("cache");
      cache.setAccessible(true);
      return (AdvancedCache<Serializable, Object>)cache.get(lockManager);
   }
}
