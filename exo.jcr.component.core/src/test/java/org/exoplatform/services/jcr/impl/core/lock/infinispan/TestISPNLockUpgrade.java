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

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.infinispan.commons.IllegalLifecycleStateException;

import jakarta.transaction.TransactionManager;

/**
 * The upgrade of the lock store of Infinispan 8 when the lock manager of a workspace starts: the workspace is removed,
 * its lock store is given the state an upgrade finds, and the workspace is added again.
 */
public class TestISPNLockUpgrade extends JcrImplBaseTest
{
   private static final String LOCKED_NODES = "SELECT * FROM mix:lockable WHERE jcr:lockOwner IS NOT NULL";

   private final TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   /**
    * A repository of its own: the shared one can hold workspaces of another database structure type.
    */
   private RepositoryImpl upgradeRepository;

   private WorkspaceEntry workspaceEntry;

   private DataSource dataSource;

   private String lockTable;

   private String claimTable;

   private String deepId;

   private String shallowId;

   private static final String LEGACY_TABLE =
      " (id VARCHAR(512) NOT NULL, data VARBINARY(65535), timestamp BIGINT NOT NULL, PRIMARY KEY (id))";

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      upgradeRepository = (RepositoryImpl)helper.createRepository(container, DatabaseStructureType.SINGLE, null);
      workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);
      helper.addWorkspace(upgradeRepository, workspaceEntry);
      dataSource = (DataSource)new InitialContext().lookup(workspaceEntry.getContainer().getParameterValue("source-name"));
      ISPNLockTableHandler tables = (ISPNLockTableHandler)lockManager().getLockTableHandler();
      lockTable = tables.getTableName("L");
      claimTable = tables.getTableName("U");

      Session locker = upgradeRepository.login(credentials, workspaceEntry.getName());
      try
      {
         Node deep = locker.getRootNode().addNode("deep");
         deep.addMixin("mix:lockable");
         deep.addNode("child");
         Node shallow = locker.getRootNode().addNode("shallow");
         shallow.addMixin("mix:lockable");
         locker.save();
         deep.lock(true, false);
         shallow.lock(false, false);
         deepId = ((NodeImpl)deep).getIdentifier();
         shallowId = ((NodeImpl)shallow).getIdentifier();
         assertEquals(2, locker.getWorkspace().getQueryManager().createQuery(LOCKED_NODES, Query.SQL).execute()
                               .getNodes().getSize());
      }
      finally
      {
         locker.logout();
      }
      upgradeRepository.removeWorkspace(workspaceEntry.getName());
   }

   @Override
   protected void tearDown() throws Exception
   {
      if (upgradeRepository.isWorkspaceInitialized(workspaceEntry.getName()))
      {
         upgradeRepository.removeWorkspace(workspaceEntry.getName());
      }
      helper.removeRepository(container, upgradeRepository.getName());
      super.tearDown();
   }

   public void testLocksOfInfinispan8AreReleasedWhenTheWorkspaceStarts() throws Exception
   {
      execute("DROP TABLE " + lockTable);
      execute("CREATE TABLE " + lockTable + LEGACY_TABLE);
      execute("INSERT INTO " + lockTable + " (id, data, timestamp) VALUES ('" + deepId + "', X'00', 0)");

      startWorkspaceAgain();

      assertReleased("deep");
      assertReleased("shallow");
      Session session = upgradeRepository.login(credentials, workspaceEntry.getName());
      try
      {
         // The index no longer holds the lock properties: the search manager started before the lock manager
         assertEquals(0, session.getWorkspace().getQueryManager().createQuery(LOCKED_NODES, Query.SQL).execute()
                                .getNodes().getSize());
         Node deep = session.getRootNode().getNode("deep");
         assertFalse(deep.isLocked());
         deep.lock(true, false);
         assertTrue(deep.isLocked());
         deep.unlock();
      }
      finally
      {
         session.logout();
      }
      assertFalse(tableExists(claimTable));
      assertTrue(hasSegmentColumn());
   }

   public void testStaleClaimIsTakenOver() throws Exception
   {
      execute("DROP TABLE " + lockTable);
      execute("CREATE TABLE " + claimTable + LEGACY_TABLE);
      claim(System.currentTimeMillis() - ISPNLockUpgrade.STALE_AFTER - 1);

      startWorkspaceAgain();

      assertReleased("deep");
      assertReleased("shallow");
      assertFalse(tableExists(claimTable));
   }

   public void testNodeLockedInTheNewStoreIsKept() throws Exception
   {
      execute("CREATE TABLE " + claimTable + LEGACY_TABLE);
      claim(System.currentTimeMillis() - ISPNLockUpgrade.STALE_AFTER - 1);
      try (Connection connection = dataSource.getConnection();
           PreparedStatement delete = connection.prepareStatement("DELETE FROM " + lockTable + " WHERE id = ?"))
      {
         delete.setString(1, shallowId);
         assertEquals(1, delete.executeUpdate());
      }

      startWorkspaceAgain();

      assertReleased("shallow");
      Session session = upgradeRepository.login(credentials, workspaceEntry.getName());
      try
      {
         assertTrue(session.getRootNode().getNode("deep").isLocked());
         assertTrue(session.getRootNode().getNode("deep").hasProperty("jcr:lockOwner"));
      }
      finally
      {
         session.logout();
      }
   }

   public void testRecentClaimIsAwaited() throws Exception
   {
      execute("DROP TABLE " + lockTable);
      execute("CREATE TABLE " + claimTable + LEGACY_TABLE);
      claim(System.currentTimeMillis());

      Thread starter = new Thread(() -> {
         try
         {
            startWorkspaceAgain();
         }
         catch (Exception e)
         {
            throw new IllegalStateException(e);
         }
      });
      starter.start();
      starter.join(3 * ISPNLockUpgrade.POLL_INTERVAL);
      assertTrue(starter.isAlive());
      assertEquals(2, countLockOwners());

      execute("DROP TABLE " + claimTable);
      starter.join(TimeUnit.MINUTES.toMillis(1));
      assertFalse(starter.isAlive());
      assertTrue(upgradeRepository.isWorkspaceInitialized(workspaceEntry.getName()));
      // The node that held the claim releases the locks, not the one that waited
      assertEquals(2, countLockOwners());
   }

   public void testRestoreBeforeTheWorkspaceStartsReplacesItsLocks() throws Exception
   {
      startWorkspaceAgain();
      File backup = Files.createTempDirectory(Path.of("target"), "locks").toFile();
      lockManager().backup(backup);
      upgradeRepository.removeWorkspace(workspaceEntry.getName());
      // A restore runs before the workspace starts
      helper.addWorkspace(upgradeRepository, workspaceEntry);

      DataRestore restorer = lockManager().getDataRestorer(new DataRestoreContext(
         new String[]{DataRestoreContext.STORAGE_DIR}, new Object[]{backup}));
      restorer.clean();
      assertEquals(0, lockManager().getLockTableHandler().getLockedNodesIds().size());
      restorer.restore();
      restorer.commit();
      restorer.close();
      assertEquals(Set.of(deepId, shallowId), lockManager().getLockTableHandler().getLockedNodesIds());
   }

   public void testStoppedLockManagerDoesNotCreateItsLockStoreAgain() throws Exception
   {
      startWorkspaceAgain();
      ISPNCacheableLockManagerImpl stopped = lockManager();
      upgradeRepository.removeWorkspace(workspaceEntry.getName());

      assertThrows(IllegalLifecycleStateException.class, () -> stopped.lockExist(deepId));
   }

   public void testReleaseSkipsTheNodesReleasedMeanwhile() throws Exception
   {
      startWorkspaceAgain();
      Session session = upgradeRepository.login(credentials, workspaceEntry.getName());
      try
      {
         // The lock properties of the node are in the cache of the workspace when another node removes them
         assertTrue(session.getRootNode().getNode("deep").hasProperty("jcr:lockOwner"));
         assertTrue(session.getRootNode().getNode("deep").hasProperty("jcr:lockIsDeep"));
      }
      finally
      {
         session.logout();
      }
      String lockProperties = "SELECT ID FROM JCR_SITEM WHERE PARENT_ID = '" + workspaceEntry.getName() + deepId
         + "' AND NAME IN ('[http://www.jcp.org/jcr/1.0]lockOwner', '[http://www.jcp.org/jcr/1.0]lockIsDeep')";
      execute("DELETE FROM JCR_SVALUE WHERE PROPERTY_ID IN (" + lockProperties + ")");
      execute("DELETE FROM JCR_SITEM WHERE ID IN (" + lockProperties + ")");

      assertEquals(1, lockManager().releaseLocks(List.of(deepId, shallowId)));
      assertReleased("shallow");
   }

   public void testUnsegmentedLockStoreIsRefused() throws Exception
   {
      WorkspaceEntry unsegmented = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);
      unsegmented.setUniqueName(upgradeRepository.getName() + "_" + unsegmented.getName());
      unsegmented.getLockManager().putParameterValue(ISPNCacheFactory.INFINISPAN_CONFIG,
         "conf/standalone/test-infinispan-lock-unsegmented.xml");
      helper.addWorkspace(upgradeRepository, workspaceEntry);
      WorkspaceContainerFacade workspaceContainer = upgradeRepository.getWorkspaceContainer(workspaceEntry.getName());

      RepositoryConfigurationException refusal = assertThrows(RepositoryConfigurationException.class,
         () -> new ISPNCacheableLockManagerImpl(
            (WorkspacePersistentDataManager)workspaceContainer.getComponent(WorkspacePersistentDataManager.class),
            unsegmented, (InitialContextInitializer)container.getComponentInstanceOfType(InitialContextInitializer.class),
            (TransactionManager)null, (ConfigurationManager)container.getComponentInstanceOfType(ConfigurationManager.class),
            (LockRemoverHolder)workspaceContainer.getComponent(LockRemoverHolder.class)));
      assertTrue(refusal.getMessage(), refusal.getMessage().contains("conf/standalone/test-infinispan-lock-unsegmented.xml"));
   }

   /**
    * Adds the workspace again and starts it, as a repository starts the workspaces of its configuration: the repository
    * does not start a workspace added again over its data.
    */
   private void startWorkspaceAgain() throws Exception
   {
      helper.addWorkspace(upgradeRepository, workspaceEntry);
      helper.getRepositoryContainer(container, upgradeRepository.getName()).getWorkspaceContainer(workspaceEntry.getName())
            .start();
   }

   private ISPNCacheableLockManagerImpl lockManager()
   {
      return (ISPNCacheableLockManagerImpl)upgradeRepository.getWorkspaceContainer(workspaceEntry.getName())
                                                    .getComponent(WorkspaceLockManager.class);
   }

   private void assertReleased(String name) throws Exception
   {
      Session session = upgradeRepository.login(credentials, workspaceEntry.getName());
      try
      {
         Node node = session.getRootNode().getNode(name);
         assertFalse(node.isLocked());
         assertFalse(node.hasProperty("jcr:lockOwner"));
         assertFalse(node.hasProperty("jcr:lockIsDeep"));
      }
      finally
      {
         session.logout();
      }
   }

   private void claim(long heartbeat) throws SQLException
   {
      try (Connection connection = dataSource.getConnection();
           PreparedStatement insert = connection.prepareStatement("INSERT INTO " + claimTable
              + " (id, data, timestamp) VALUES (?, ?, ?)"))
      {
         insert.setString(1, ISPNLockUpgrade.CLAIM_ID);
         insert.setBytes(2, "crashed".getBytes(StandardCharsets.UTF_8));
         insert.setLong(3, heartbeat);
         insert.executeUpdate();
      }
   }

   private int countLockOwners() throws SQLException
   {
      try (Connection connection = dataSource.getConnection();
           Statement statement = connection.createStatement();
           ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM JCR_SITEM WHERE CONTAINER_NAME = '"
              + workspaceEntry.getName() + "' AND NAME = '[http://www.jcp.org/jcr/1.0]lockOwner'"))
      {
         count.next();
         return count.getInt(1);
      }
   }

   private boolean hasSegmentColumn() throws SQLException
   {
      try (Connection connection = dataSource.getConnection();
           Statement statement = connection.createStatement();
           ResultSet empty = statement.executeQuery("SELECT * FROM " + lockTable + " WHERE 1 = 0"))
      {
         for (int i = 1; i <= empty.getMetaData().getColumnCount(); i++)
         {
            if (empty.getMetaData().getColumnName(i).equalsIgnoreCase("segment_id"))
            {
               return true;
            }
         }
         return false;
      }
   }

   private boolean tableExists(String table) throws SQLException
   {
      try (Connection connection = dataSource.getConnection())
      {
         return JDBCUtils.tableExists(table, connection);
      }
   }

   private void execute(String sql) throws SQLException
   {
      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate(sql);
      }
   }
}
