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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.hsqldb.jdbc.JDBCDataSource;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The claim of the upgrade of a lock store between nodes that start together, each one an upgrade on the same
 * database: the rename of the old table that one node wins, the claim left without its reserved row, the takeover of a
 * stale claim that one node wins, the claimer replaced by another node, and the old table left beside a claim. On
 * HSQLDB here, and on the engines of the integration tests that extend it.
 */
public class TestISPNLockUpgradeClaim
{
   private static final long STALE_AFTER = 300;

   private static final long POLL_INTERVAL = 20;

   /**
    * A schema of another platform on the same database, which the account can see.
    */
   protected static final String OTHER_SCHEMA = "other_platform";

   private final List<List<String>> releases = Collections.synchronizedList(new ArrayList<>());

   private final ExecutorService executor = Executors.newCachedThreadPool();

   private DataSource dataSource;

   private WorkspaceEntry workspace;

   private TableManipulationConfiguration table;

   private String lockTable;

   private String claimTable;

   /**
    * The columns of the lock table of Infinispan 8.
    */
   private String legacyTable;

   @Before
   public void createDatabase() throws Exception
   {
      dataSource = createDataSource();
      workspace = new WorkspaceEntry();
      workspace.setName("ws");
      workspace.setUniqueName("repo_ws");
      workspace.setContainer(new ContainerEntry(JDBCWorkspaceDataContainer.class.getName(),
         new ArrayList<>(List.of(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE, "single")))));
      LockManagerEntry lockManager = new LockManagerEntry();
      lockManager.putParameterValue(ISPNCacheableLockManagerImpl.INFINISPAN_JDBC_TABLE_NAME, "lk");
      lockManager.putParameterValue(ISPNCacheableLockManagerImpl.INFINISPAN_JDBC_CL_ID_COLUMN_NAME, "id");
      workspace.setLockManager(lockManager);
      JdbcStringBasedStoreConfigurationBuilder store =
         new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      table = store.table()
                   .tableNamePrefix("lk")
                   .idColumnName("id")
                   .dataColumnName("data")
                   .timestampColumnName("timestamp")
                   .segmentColumnName("segment_id")
                   .create();
      ISPNLockTableHandler tables = new ISPNLockTableHandler(workspace, dataSource);
      lockTable = tables.getTableName("L");
      claimTable = tables.getTableName("U");
      legacyTable = " (id VARCHAR(512) NOT NULL, data " + binaryType()
         + ", timestamp BIGINT NOT NULL, PRIMARY KEY (id))";

      execute("CREATE TABLE JCR_SITEM (ID VARCHAR(96) NOT NULL, PARENT_ID VARCHAR(96), NAME VARCHAR(512),"
         + " I_CLASS INTEGER, CONTAINER_NAME VARCHAR(96), PRIMARY KEY (ID))");
      for (String node : List.of("node1", "node2"))
      {
         execute("INSERT INTO JCR_SITEM VALUES ('ws" + node + "owner', 'ws" + node
            + "', '[http://www.jcp.org/jcr/1.0]lockOwner', 2, 'ws')");
      }
   }

   @After
   public void stop() throws Exception
   {
      executor.shutdownNow();
      executor.awaitTermination(1, TimeUnit.MINUTES);
      dropDatabase();
   }

   /**
    * The database of a test, an in-memory HSQLDB one.
    */
   protected DataSource createDataSource() throws SQLException
   {
      JDBCDataSource hsqldb = new JDBCDataSource();
      hsqldb.setUrl("jdbc:hsqldb:mem:" + UUID.randomUUID());
      hsqldb.setUser("sa");
      hsqldb.setPassword("");
      return hsqldb;
   }

   /**
    * The type of the data column of the lock table of Infinispan 8 on the engine.
    */
   protected String binaryType()
   {
      return "VARBINARY(65535)";
   }

   protected void dropDatabase() throws SQLException
   {
      execute("SHUTDOWN");
   }

   /**
    * Drops the tables of a test, on an engine whose database outlives the test.
    */
   protected void dropTables() throws SQLException
   {
      for (String name : List.of(claimTable, lockTable, "JCR_SITEM"))
      {
         if (tableExists(name))
         {
            execute("DROP TABLE " + name);
         }
      }
   }

   @Test
   public void testOneNodeWinsTheRename() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);

      DataSource together = together("ALTER TABLE");
      List<Boolean> upgraded = runTogether(upgrade(together), upgrade(together));

      assertEquals(1, upgraded.stream().filter(Boolean::booleanValue).count());
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(claimTable));
   }

   @Test
   public void testOneNodeTakesAStaleClaimOver() throws Exception
   {
      execute("CREATE TABLE " + claimTable + legacyTable);
      claim(System.currentTimeMillis() - STALE_AFTER);

      DataSource together = together("UPDATE");
      List<Boolean> upgraded = runTogether(upgrade(together), upgrade(together));

      assertEquals(1, upgraded.stream().filter(Boolean::booleanValue).count());
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(claimTable));
   }

   @Test
   public void testClaimLeftWithoutItsRowIsTakenAfterTheBound() throws Exception
   {
      execute("CREATE TABLE " + claimTable + legacyTable);

      long start = System.currentTimeMillis();
      assertTrue(upgrade(dataSource).run());

      assertTrue(System.currentTimeMillis() - start >= STALE_AFTER);
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(claimTable));
   }

   @Test
   public void testClaimIsAwaitedBeforeTheOldTableIsRenamed() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      execute("CREATE TABLE " + claimTable + legacyTable);
      claim(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

      AtomicInteger renames = new AtomicInteger();
      DataSource counted = onStatement("ALTER TABLE", renames::incrementAndGet);
      Future<Boolean> upgraded = executor.submit(() -> upgrade(counted).run());
      Thread.sleep(3 * STALE_AFTER);
      assertFalse(upgraded.isDone());
      assertTrue(releases.isEmpty());
      assertEquals("No rename while the claim is held", 0, renames.get());

      execute("DROP TABLE " + claimTable);
      assertTrue(upgraded.get(1, TimeUnit.MINUTES));
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertEquals(1, renames.get());
   }

   @Test
   public void testANodeThatStopsWhileTheClaimIsHeldStopsWaiting() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      execute("CREATE TABLE " + claimTable + legacyTable);
      claim(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
      AtomicBoolean stopped = new AtomicBoolean();
      ISPNLockUpgrade upgrade = new ISPNLockUpgrade(workspace, dataSource, dataSource, table, this::release, stopped::get,
                                                    STALE_AFTER, POLL_INTERVAL, ISPNLockUpgrade.BATCH_SIZE);
      Future<Boolean> upgraded = executor.submit(upgrade::run);
      Thread.sleep(3 * STALE_AFTER);
      assertFalse(upgraded.isDone());

      stopped.set(true);

      // The node stops waiting at its next poll, and the claim stays with the node that holds it
      ExecutionException failure = assertThrows(ExecutionException.class, () -> upgraded.get(5, TimeUnit.SECONDS));
      assertTrue(String.valueOf(failure.getCause()), failure.getCause() instanceof javax.jcr.RepositoryException);
      assertTrue(tableExists(claimTable));
      assertTrue(releases.isEmpty());
   }

   @Test
   public void testClaimerReplacedByAnotherNodeStops() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);

      boolean upgraded = upgrade(dataSource, nodes -> {
         releases.add(List.copyOf(nodes));
         // Another node takes the claim over meanwhile, then ends the upgrade
         heartbeat(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
         executor.submit(() -> {
            Thread.sleep(3 * STALE_AFTER);
            execute("DROP TABLE " + claimTable);
            return null;
         });
         return nodes.size();
      }).run();

      assertFalse(upgraded);
      assertEquals(1, releases.size());
   }

   @Test
   public void testANodeLockedInTheNewStoreDuringTheUpgradeKeepsItsLock() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);

      boolean upgraded = new ISPNLockUpgrade(workspace, dataSource, dataSource, table, nodes -> {
         releases.add(List.copyOf(nodes));
         if (releases.size() > 1)
         {
            return nodes.size();
         }
         try
         {
            // A node already started creates the new store, with its segment column, and locks node2 before its
            // batch
            execute("CREATE TABLE " + lockTable + " (id VARCHAR(512) NOT NULL, data " + binaryType()
               + ", timestamp BIGINT NOT NULL, segment_id INTEGER NOT NULL, PRIMARY KEY (id))");
            execute("INSERT INTO " + lockTable + " (id, timestamp, segment_id) VALUES ('node2', 0, 0)");
         }
         catch (SQLException e)
         {
            throw new javax.jcr.RepositoryException(e);
         }
         return nodes.size();
      }, STALE_AFTER, POLL_INTERVAL, 1).run();

      assertTrue(upgraded);
      assertEquals(List.of(List.of("node1")), releases);
   }

   @Test
   public void testAFailedReadOfTheNewStoreStopsTheUpgradeAndKeepsItsClaim() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);

      ISPNLockUpgrade upgrade = new ISPNLockUpgrade(workspace, dataSource, dataSource, table, nodes -> {
         releases.add(List.copyOf(nodes));
         try
         {
            // The new store cannot be read from the next batch on
            execute("CREATE TABLE " + lockTable + " (lock_key VARCHAR(512), segment_id INTEGER NOT NULL)");
            execute("INSERT INTO " + lockTable + " VALUES ('node2', 0)");
         }
         catch (SQLException e)
         {
            throw new javax.jcr.RepositoryException(e);
         }
         return nodes.size();
      }, STALE_AFTER, POLL_INTERVAL, 1);

      assertThrows(javax.jcr.RepositoryException.class, upgrade::run);
      assertEquals(List.of(List.of("node1")), releases);
      assertTrue(tableExists(claimTable));
   }

   @Test
   public void testANewStoreWhoseExistenceQueryFailsKeepsItsLocks() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      AtomicBoolean created = new AtomicBoolean();
      // Once the new store exists, a query that counts its rows fails, as a query an existence check runs may
      DataSource failing = failing(created, lockTable, false);

      boolean upgraded = new ISPNLockUpgrade(workspace, failing, dataSource, table, nodes -> {
         releases.add(List.copyOf(nodes));
         createNewStoreLocking("node2", created);
         return nodes.size();
      }, STALE_AFTER, POLL_INTERVAL, 1).run();

      assertTrue(upgraded);
      assertEquals(List.of(List.of("node1")), releases);
   }

   @Test
   public void testAFailedLookupOfTheNewStoreStopsTheUpgradeAndKeepsItsClaim() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      AtomicBoolean created = new AtomicBoolean();
      // Once the new store exists, the database metadata cannot be read
      DataSource failing = failing(created, lockTable, true);

      ISPNLockUpgrade upgrade = new ISPNLockUpgrade(workspace, failing, dataSource, table, nodes -> {
         releases.add(List.copyOf(nodes));
         createNewStoreLocking("node2", created);
         return nodes.size();
      }, STALE_AFTER, POLL_INTERVAL, 1);

      assertThrows(javax.jcr.RepositoryException.class, upgrade::run);
      assertEquals(List.of(List.of("node1")), releases);
      assertTrue(tableExists(claimTable));
   }

   @Test
   public void testAWaitingNodeWhoseClaimQueryFailsKeepsWaiting() throws Exception
   {
      // Another node renamed the old table and holds the claim
      execute("CREATE TABLE " + claimTable + legacyTable);
      claim(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
      // A query that counts the claim table's rows fails, as a query an existence check runs may
      DataSource failing = failing(new AtomicBoolean(true), claimTable, false);

      // The node waits for the claim, and stops waiting since it is asked to stop, instead of starting its lock store
      ISPNLockUpgrade upgrade = new ISPNLockUpgrade(workspace, failing, dataSource, table, this::release, () -> true,
                                                    STALE_AFTER, POLL_INTERVAL, ISPNLockUpgrade.BATCH_SIZE);

      assertThrows(javax.jcr.RepositoryException.class, upgrade::run);
      assertTrue(tableExists(claimTable));
      assertTrue(releases.isEmpty());
   }

   /**
    * Creates the new store, with its segment column, as a node already started does, and locks the given node in it.
    */
   private void createNewStoreLocking(String node, AtomicBoolean created) throws javax.jcr.RepositoryException
   {
      try
      {
         execute("CREATE TABLE " + lockTable + " (id VARCHAR(512) NOT NULL, data " + binaryType()
            + ", timestamp BIGINT NOT NULL, segment_id INTEGER NOT NULL, PRIMARY KEY (id))");
         execute("INSERT INTO " + lockTable + " (id, timestamp, segment_id) VALUES ('" + node + "', 0, 0)");
         created.set(true);
      }
      catch (SQLException e)
      {
         throw new javax.jcr.RepositoryException(e);
      }
   }

   /**
    * The data source, which, once the flag is set, fails each query that counts the rows of the given table, in each
    * form the existence check of {@link org.exoplatform.services.database.utils.JDBCUtils} sends it (a plain count, or a
    * count of a subquery limited to one row on the other engines), and, when asked, each read of the database metadata.
    */
   private DataSource failing(AtomicBoolean active, String countedTable, boolean failMetadata)
   {
      String table = countedTable.toLowerCase(Locale.ROOT);
      return proxy(DataSource.class, dataSource, (target, method, args) -> {
         Object result = invoke(target, method, args);
         if (!method.getName().equals("getConnection"))
         {
            return result;
         }
         return proxy(Connection.class, (Connection) result, (connection, connectionMethod, connectionArgs) -> {
            if (active.get() && failMetadata && connectionMethod.getName().equals("getMetaData"))
            {
               throw new SQLException("The database metadata cannot be read");
            }
            Object connectionResult = invoke(connection, connectionMethod, connectionArgs);
            if (!connectionMethod.getName().equals("createStatement"))
            {
               return connectionResult;
            }
            return proxy(Statement.class, (Statement) connectionResult, (sql, sqlMethod, sqlArgs) -> {
               if (active.get() && sqlMethod.getName().startsWith("execute") && sqlArgs != null
                  && isCount(((String) sqlArgs[0]).toLowerCase(Locale.ROOT), table))
               {
                  throw new SQLException("The rows of " + countedTable + " cannot be counted");
               }
               return invoke(sql, sqlMethod, sqlArgs);
            });
         });
      });
   }

   @Test
   public void testANewStoreOfAnotherSchemaIsNotThisWorkspaces() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      // Another platform on the same database has upgraded its own lock store, which locks node2: Infinispan, which
      // looks its table up in every schema, would take it for this workspace's once the old table is renamed
      createTheNewStoreOfAnotherSchema();

      RepositoryException e = assertThrows(RepositoryException.class, () -> upgrade(dataSource).run());

      assertTrue(e.getMessage(), e.getMessage().contains("nullDatabaseMeansCurrent=true"));
      // A new prefix would leave the old table's locks behind
      assertTrue(e.getMessage(), e.getMessage().contains("database of its own"));
      assertTrue(e.getMessage(), e.getMessage().toUpperCase(Locale.ROOT).contains(OTHER_SCHEMA.toUpperCase(Locale.ROOT)));
      // Nothing is claimed nor released
      assertEquals(List.of(), releases);
      assertTrue(tableExists(lockTable));
      assertFalse(tableExists(claimTable));
   }

   @Test
   public void testAWorkspaceWithoutLockStoreDoesNotStartWhenInfinispanWouldTakeAnotherSchemasOne() throws Exception
   {
      createTheNewStoreOfAnotherSchema();

      RepositoryException e = assertThrows(RepositoryException.class, () -> upgrade(dataSource).run());

      assertTrue(e.getMessage(), e.getMessage().contains("prefix that no other schema"));
   }

   @Test
   public void testAWorkspaceWithItsOwnLockStoreStartsBesideAnotherSchemasOne() throws Exception
   {
      execute("CREATE TABLE " + lockTable + " (id VARCHAR(512) NOT NULL, data " + binaryType()
         + ", timestamp BIGINT NOT NULL, segment_id INTEGER NOT NULL, PRIMARY KEY (id))");
      createTheNewStoreOfAnotherSchema();

      assertFalse(upgrade(dataSource).run());

      assertEquals(List.of(), releases);
   }

   protected void createTheNewStoreOfAnotherSchema() throws SQLException
   {
      execute("CREATE SCHEMA " + OTHER_SCHEMA);
      execute("CREATE TABLE " + OTHER_SCHEMA + "." + lockTable + " (id VARCHAR(512) NOT NULL, data " + binaryType()
         + ", timestamp BIGINT NOT NULL, segment_id INTEGER NOT NULL, PRIMARY KEY (id))");
      execute("INSERT INTO " + OTHER_SCHEMA + "." + lockTable + " (id, timestamp, segment_id) VALUES ('node2', 0, 0)");
   }

   @Test
   public void testOldTableLeftBesideAStaleClaimIsUpgradedToo() throws Exception
   {
      execute("CREATE TABLE " + lockTable + legacyTable);
      execute("CREATE TABLE " + claimTable + legacyTable);
      claim(System.currentTimeMillis() - STALE_AFTER);

      assertTrue(upgrade(dataSource).run());

      assertEquals(List.of(List.of("node1", "node2"), List.of("node1", "node2")), releases);
      assertFalse(tableExists(lockTable));
      assertFalse(tableExists(claimTable));
   }

   protected ISPNLockUpgrade upgrade(DataSource lockDataSource)
   {
      return upgrade(lockDataSource, this::release);
   }

   private ISPNLockUpgrade upgrade(DataSource lockDataSource, ISPNLockUpgrade.LockReleaser releaser)
   {
      return new ISPNLockUpgrade(workspace, lockDataSource, dataSource, table, releaser, STALE_AFTER, POLL_INTERVAL);
   }

   private int release(List<String> nodes)
   {
      releases.add(nodes.stream().sorted().toList());
      return nodes.size();
   }


   private static boolean isCount(String sql, String table)
   {
      return sql.startsWith("select count(*)") && sql.matches("(?s).* from " + Pattern.quote(table) + "([ )].*)?");
   }

   /**
    * The data source, whose first two statements that start with the given text are held until both are sent: two
    * nodes that read the same state before either one acts on it.
    */
   private DataSource together(String statement)
   {
      CyclicBarrier barrier = new CyclicBarrier(2);
      AtomicInteger held = new AtomicInteger();
      Runnable await = () -> {
         if (held.getAndIncrement() < 2)
         {
            try
            {
               barrier.await(10, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
               throw new IllegalStateException("The other node did not send " + statement, e);
            }
         }
      };
      return onStatement(statement, await);
   }

   /**
    * The data source, which runs the given action before each statement that starts with the given text.
    */
   private DataSource onStatement(String statement, Runnable action)
   {
      return proxy(DataSource.class, dataSource, (target, method, args) -> {
         Object result = invoke(target, method, args);
         if (!method.getName().equals("getConnection"))
         {
            return result;
         }
         return proxy(Connection.class, (Connection) result, (connection, connectionMethod, connectionArgs) -> {
            if (connectionMethod.getName().equals("prepareStatement")
               && ((String) connectionArgs[0]).startsWith(statement))
            {
               action.run();
            }
            Object connectionResult = invoke(connection, connectionMethod, connectionArgs);
            if (!connectionMethod.getName().equals("createStatement"))
            {
               return connectionResult;
            }
            return proxy(Statement.class, (Statement) connectionResult, (sql, sqlMethod, sqlArgs) -> {
               if (sqlMethod.getName().startsWith("execute") && sqlArgs != null
                  && ((String) sqlArgs[0]).startsWith(statement))
               {
                  action.run();
               }
               return invoke(sql, sqlMethod, sqlArgs);
            });
         });
      });
   }

   private interface Handler<T>
   {
      Object invoke(T target, Method method, Object[] args) throws Throwable;
   }

   private static <T> T proxy(Class<T> type, T target, Handler<T> handler)
   {
      InvocationHandler invocation = (proxy, method, args) -> handler.invoke(target, method, args);
      return type.cast(Proxy.newProxyInstance(TestISPNLockUpgradeClaim.class.getClassLoader(), new Class<?>[]{type},
         invocation));
   }

   private static Object invoke(Object target, Method method, Object[] args) throws Throwable
   {
      try
      {
         return method.invoke(target, args);
      }
      catch (InvocationTargetException e)
      {
         throw e.getCause();
      }
   }

   private List<Boolean> runTogether(ISPNLockUpgrade first, ISPNLockUpgrade second) throws Exception
   {
      List<Future<Boolean>> runs =
         executor.invokeAll(List.<Callable<Boolean>>of(first::run, second::run), 1, TimeUnit.MINUTES);
      List<Boolean> upgraded = new ArrayList<>();
      for (Future<Boolean> run : runs)
      {
         upgraded.add(run.get());
      }
      return upgraded;
   }

   private void claim(long heartbeat) throws SQLException
   {
      try (Connection connection = dataSource.getConnection();
           PreparedStatement insert = connection.prepareStatement("INSERT INTO " + claimTable
              + " (id, data, timestamp) VALUES (?, ?, ?)"))
      {
         insert.setString(1, ISPNLockUpgrade.CLAIM_ID);
         insert.setBytes(2, new byte[]{0});
         insert.setLong(3, heartbeat);
         insert.executeUpdate();
      }
   }

   private void heartbeat(long heartbeat)
   {
      try (Connection connection = dataSource.getConnection();
           PreparedStatement update = connection.prepareStatement("UPDATE " + claimTable
              + " SET timestamp = ? WHERE id = ?"))
      {
         update.setLong(1, heartbeat);
         update.setString(2, ISPNLockUpgrade.CLAIM_ID);
         assertEquals(1, update.executeUpdate());
      }
      catch (SQLException e)
      {
         throw new IllegalStateException(e);
      }
   }

   protected boolean tableExists(String name) throws SQLException
   {
      try (Connection connection = dataSource.getConnection())
      {
         return JDBCUtils.tableExists(name, connection);
      }
   }

   protected void execute(String sql) throws SQLException
   {
      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate(sql);
      }
   }
}
