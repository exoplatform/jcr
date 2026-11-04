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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
 * stale claim that one node wins, the claimer replaced by another node, and the old table left beside a claim.
 */
public class TestISPNLockUpgradeClaim
{
   private static final long STALE_AFTER = 300;

   private static final long POLL_INTERVAL = 20;

   private static final String LEGACY_TABLE =
      " (id VARCHAR(512) NOT NULL, data VARBINARY(65535), timestamp BIGINT NOT NULL, PRIMARY KEY (id))";

   private static final String LOCK_TABLE = "\"lk_Lrepows\"";

   private static final String CLAIM_TABLE = "\"lk_Urepows\"";

   private final List<List<String>> releases = Collections.synchronizedList(new ArrayList<>());

   private final ExecutorService executor = Executors.newCachedThreadPool();

   private JDBCDataSource dataSource;

   private WorkspaceEntry workspace;

   private TableManipulationConfiguration table;

   @Before
   public void createDatabase() throws Exception
   {
      dataSource = new JDBCDataSource();
      dataSource.setUrl("jdbc:hsqldb:mem:" + UUID.randomUUID());
      dataSource.setUser("sa");
      dataSource.setPassword("");
      execute("CREATE TABLE JCR_SITEM (ID VARCHAR(96) NOT NULL, PARENT_ID VARCHAR(96), NAME VARCHAR(512),"
         + " I_CLASS INTEGER, CONTAINER_NAME VARCHAR(96), PRIMARY KEY (ID))");
      for (String node : List.of("node1", "node2"))
      {
         execute("INSERT INTO JCR_SITEM VALUES ('ws" + node + "owner', 'ws" + node
            + "', '[http://www.jcp.org/jcr/1.0]lockOwner', 2, 'ws')");
      }

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
   }

   @After
   public void stop() throws Exception
   {
      executor.shutdownNow();
      execute("SHUTDOWN");
   }

   @Test
   public void testOneNodeWinsTheRename() throws Exception
   {
      execute("CREATE TABLE " + LOCK_TABLE + LEGACY_TABLE);

      DataSource together = together("ALTER TABLE");
      List<Boolean> upgraded = runTogether(upgrade(together), upgrade(together));

      assertEquals(1, upgraded.stream().filter(Boolean::booleanValue).count());
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(CLAIM_TABLE));
   }

   @Test
   public void testOneNodeTakesAStaleClaimOver() throws Exception
   {
      execute("CREATE TABLE " + CLAIM_TABLE + LEGACY_TABLE);
      claim(System.currentTimeMillis() - STALE_AFTER);

      DataSource together = together("UPDATE");
      List<Boolean> upgraded = runTogether(upgrade(together), upgrade(together));

      assertEquals(1, upgraded.stream().filter(Boolean::booleanValue).count());
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(CLAIM_TABLE));
   }

   @Test
   public void testClaimLeftWithoutItsRowIsTakenAfterTheBound() throws Exception
   {
      execute("CREATE TABLE " + CLAIM_TABLE + LEGACY_TABLE);

      long start = System.currentTimeMillis();
      assertTrue(upgrade(dataSource).run());

      assertTrue(System.currentTimeMillis() - start >= STALE_AFTER);
      assertEquals(List.of(List.of("node1", "node2")), releases);
      assertFalse(tableExists(CLAIM_TABLE));
   }

   @Test
   public void testClaimIsAwaitedBeforeTheOldTableIsRenamed() throws Exception
   {
      execute("CREATE TABLE " + LOCK_TABLE + LEGACY_TABLE);
      execute("CREATE TABLE " + CLAIM_TABLE + LEGACY_TABLE);
      claim(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

      Future<Boolean> upgraded = executor.submit(() -> upgrade(dataSource).run());
      Thread.sleep(3 * STALE_AFTER);
      assertFalse(upgraded.isDone());
      assertTrue(releases.isEmpty());

      execute("DROP TABLE " + CLAIM_TABLE);
      assertTrue(upgraded.get(1, TimeUnit.MINUTES));
      assertEquals(List.of(List.of("node1", "node2")), releases);
   }

   @Test
   public void testClaimerReplacedByAnotherNodeStops() throws Exception
   {
      execute("CREATE TABLE " + LOCK_TABLE + LEGACY_TABLE);

      boolean upgraded = upgrade(dataSource, nodes -> {
         releases.add(List.copyOf(nodes));
         // Another node takes the claim over meanwhile, then ends the upgrade
         heartbeat(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
         executor.submit(() -> {
            Thread.sleep(3 * STALE_AFTER);
            execute("DROP TABLE " + CLAIM_TABLE);
            return null;
         });
         return nodes.size();
      }).run();

      assertFalse(upgraded);
      assertEquals(1, releases.size());
   }

   @Test
   public void testOldTableLeftBesideAStaleClaimIsUpgradedToo() throws Exception
   {
      execute("CREATE TABLE " + LOCK_TABLE + LEGACY_TABLE);
      execute("CREATE TABLE " + CLAIM_TABLE + LEGACY_TABLE);
      claim(System.currentTimeMillis() - STALE_AFTER);

      assertTrue(upgrade(dataSource).run());

      assertEquals(List.of(List.of("node1", "node2"), List.of("node1", "node2")), releases);
      assertFalse(tableExists(LOCK_TABLE));
      assertFalse(tableExists(CLAIM_TABLE));
   }

   private ISPNLockUpgrade upgrade(DataSource lockDataSource)
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
               await.run();
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
                  await.run();
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
           PreparedStatement insert = connection.prepareStatement("INSERT INTO " + CLAIM_TABLE
              + " (id, data, timestamp) VALUES (?, X'00', ?)"))
      {
         insert.setString(1, ISPNLockUpgrade.CLAIM_ID);
         insert.setLong(2, heartbeat);
         insert.executeUpdate();
      }
   }

   private void heartbeat(long heartbeat)
   {
      try (Connection connection = dataSource.getConnection();
           PreparedStatement update = connection.prepareStatement("UPDATE " + CLAIM_TABLE
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

   private boolean tableExists(String name) throws SQLException
   {
      try (Connection connection = dataSource.getConnection())
      {
         return JDBCUtils.tableExists(name, connection);
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
