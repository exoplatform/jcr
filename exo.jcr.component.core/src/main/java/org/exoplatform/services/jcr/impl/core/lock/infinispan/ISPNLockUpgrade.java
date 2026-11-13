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

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;

/**
 * The upgrade of the lock store of a workspace from the table of Infinispan 8, which has no segment column, to the
 * segmented table of Infinispan 16. The locks of the old table are released: the lock properties of the nodes of the
 * workspace are removed, as the lock remover removes those of an expired lock, and the old table is dropped.
 * <p>
 * A node claims the upgrade for the cluster by renaming the old table to a claim table, which only one node can do. A
 * reserved row of the claim table holds the claiming node and its heartbeat, updated after each batch of nodes. The
 * other nodes wait until the claim table is gone, and take the upgrade over with a conditional update of the reserved
 * row when its heartbeat gets stale: each batch skips the nodes already locked in the new table, so the upgrade can be
 * run again. A failure to read the new table stops the upgrade, which keeps its claim for another node, or the next
 * start, to take over.
 * <p>
 * The new table is read before each batch, not within its release: a node locked, by a node already started on the
 * new store, between that read and the release of its batch keeps its lock in the new table and loses its lock
 * properties. The window is the release of one batch, while the first nodes of a cluster start on the new store.
 * <p>
 * The upgrade runs in the thread that starts the lock manager, which holds the monitor of the manager. A node that
 * stops while it waits for another node's upgrade stops waiting, so that its stop does not wait for that upgrade.
 */
class ISPNLockUpgrade
{
   /**
    * The id of the reserved row of the claim table.
    */
   static final String CLAIM_ID = "__exo_lock_upgrade__";

   /**
    * The age of a heartbeat from which the claim is taken over.
    */
   static final long STALE_AFTER = TimeUnit.MINUTES.toMillis(2);

   /**
    * The time a node waits before it reads the claim again.
    */
   static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(1);

   static final int BATCH_SIZE = 100;

   /**
    * The heartbeat read when the claim table was dropped meanwhile.
    */
   private static final long GONE = Long.MAX_VALUE;

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ISPNLockUpgrade");

   /**
    * Releases the locks of nodes.
    */
   interface LockReleaser
   {
      /**
       * @param nodeIdentifiers the identifiers of the nodes
       * @return the number of nodes whose lock properties were removed
       */
      int release(List<String> nodeIdentifiers) throws RepositoryException;
   }

   private final WorkspaceEntry workspace;

   private final ISPNLockTableHandler tables;

   private final DataSource lockDataSource;

   private final DataSource itemDataSource;

   private final TableManipulationConfiguration table;

   private final LockReleaser releaser;

   private final String node;

   private final long staleAfter;

   private final long pollInterval;

   private final int batchSize;

   private final BooleanSupplier stopRequested;

   private long heartbeat;

   /**
    * @param workspace the workspace
    * @param lockDataSource the data source of the lock table
    * @param itemDataSource the data source of the items of the workspace
    * @param table the table of the lock store
    * @param releaser the releaser of the locks of the nodes
    * @param stopRequested whether the lock manager stops, which ends a wait for another node's upgrade
    */
   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser, BooleanSupplier stopRequested)
   {
      this(workspace, lockDataSource, itemDataSource, table, releaser, stopRequested, STALE_AFTER, POLL_INTERVAL,
         BATCH_SIZE);
   }

   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser, long staleAfter, long pollInterval)
   {
      this(workspace, lockDataSource, itemDataSource, table, releaser, () -> false, staleAfter, pollInterval,
         BATCH_SIZE);
   }

   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser, long staleAfter, long pollInterval, int batchSize)
   {
      this(workspace, lockDataSource, itemDataSource, table, releaser, () -> false, staleAfter, pollInterval,
         batchSize);
   }

   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser, BooleanSupplier stopRequested, long staleAfter,
      long pollInterval, int batchSize)
   {
      this.stopRequested = stopRequested;
      this.batchSize = batchSize;
      this.workspace = workspace;
      this.tables = new ISPNLockTableHandler(workspace, lockDataSource, table);
      this.lockDataSource = lockDataSource;
      this.itemDataSource = itemDataSource;
      this.table = table;
      this.releaser = releaser;
      this.node = ManagementFactory.getRuntimeMXBean().getName();
      this.staleAfter = staleAfter;
      this.pollInterval = pollInterval;
   }

   /**
    * Upgrades the lock store when the lock table has no segment column or when a claim exists, and waits for the node
    * that holds the claim otherwise, until neither the claim nor a lock table without segment column is left.
    *
    * @return whether this node upgraded the lock store
    */
   boolean run() throws RepositoryException
   {
      try
      {
         String lockTable = tables.getTableName("L");
         String claimTable = tables.getTableName("U");
         long claimWithoutRowSince = 0;
         boolean waiting = false;
         boolean upgraded = false;
         try (Connection connection = lockDataSource.getConnection())
         {
            refuseALookupThatMisleadsInfinispan(connection, lockTable);
         }
         while (true)
         {
            try (Connection connection = lockDataSource.getConnection())
            {
               // A claim is awaited first: a node that lost the race to the claim reads it here
               if (!tableExists(connection, claimTable))
               {
                  if (!isLegacy(connection, lockTable))
                  {
                     return upgraded;
                  }
                  upgraded |= claim(connection, lockTable, claimTable) && upgrade(claimTable);
                  continue;
               }
               Long claimed = readHeartbeat(connection, claimTable);
               long now = System.currentTimeMillis();
               if (claimed == null)
               {
                  // The claiming node renames the table and then adds the reserved row
                  claimWithoutRowSince = claimWithoutRowSince == 0 ? now : claimWithoutRowSince;
                  if (now - claimWithoutRowSince >= staleAfter)
                  {
                     upgraded |= insertClaimUnlessPresent(connection, claimTable) && upgrade(claimTable);
                     continue;
                  }
               }
               else if (claimed == GONE)
               {
                  continue;
               }
               else if (now - claimed >= staleAfter)
               {
                  // A lock table without segment column left beside the claim is upgraded next
                  upgraded |= takeOver(connection, claimTable, claimed) && upgrade(claimTable);
                  continue;
               }
               if (!waiting)
               {
                  LOG.info("Waiting for another node to upgrade the lock store of the workspace {}",
                     workspace.getUniqueName());
                  waiting = true;
               }
               if (stopRequested.getAsBoolean())
               {
                  // The claim stays with the node that holds it
                  throw new RepositoryException("The lock manager of the workspace " + workspace.getUniqueName()
                     + " stopped while another node upgrades its lock store");
               }
            }
            Thread.sleep(pollInterval);
         }
      }
      catch (SQLException | RepositoryConfigurationException e)
      {
         throw new RepositoryException("Cannot upgrade the lock store of the workspace " + workspace.getUniqueName(),
            e);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new RepositoryException("Interrupted while upgrading the lock store of the workspace "
            + workspace.getUniqueName(), e);
      }
   }

   /**
    * Refuses to start when the lock store of this workspace has no table in the database or schema of the connection
    * yet, or only its table without segment column, which the upgrade renames, while Infinispan's lookup finds a
    * table of that name elsewhere. Infinispan looks the table up with no catalog: MySQL Connector/J then reads every
    * database the account can read, unless its URL sets <code>nullDatabaseMeansCurrent=true</code>, and PostgreSQL or
    * HSQLDB every schema of the database. Infinispan would take that table for its own and not create it here, and
    * every lock would fail. The tables are left as they are.
    * <p>
    * On Oracle and DB2, Infinispan looks the table up in the schema of the user or the current one: no guard is
    * needed. On PostgreSQL, a table of this workspace in a schema of the search path other than the current one is
    * not this workspace's for the guard, which refuses: Infinispan reads and writes the table of the search path, but
    * the guard cannot tell it from another platform's.
    * <p>
    * A pool whose connections cannot name their schema (a JDBC 4.0 pool, such as Commons DBCP 1) is not guarded: every
    * schema then reads as the connection's own, so the guard lets the workspace start as it did before it existed.
    */
   private void refuseALookupThatMisleadsInfinispan(Connection connection, String lockTable)
      throws SQLException, RepositoryException
   {
      if (!lookedUpWithoutSchema(DialectDetecter.detect(connection.getMetaData())))
      {
         return;
      }
      boolean legacy = isLegacy(connection, lockTable);
      if (tableExists(connection, lockTable) && !legacy)
      {
         return;
      }
      String elsewhere = findElsewhere(connection, lockTable);
      if (elsewhere != null)
      {
         throw new RepositoryException("The lock store of the workspace " + workspace.getUniqueName()
            + " cannot start: its table " + lockTable + " is not in the database of the connection yet, and the"
            + " lookup of Infinispan, which names no database, finds the table " + elsewhere
            + " instead, which it would take for its own. On MySQL, add nullDatabaseMeansCurrent=true to the URL of"
            + " the data source, or use an account that reads this database only. On another database, "
            + (legacy
               ? "move the tables of the other platform to a database of its own: a new prefix would leave the locks of"
                  + " the old table behind, on nodes that keep their lock properties and cannot be locked again until"
                  + " the repository check repairs them."
               : "give the lock tables a prefix that no other schema of the database uses."));
      }
   }

   /**
    * @return whether Infinispan looks the lock table up with no schema on the database of the dialect: Oracle and DB2
    *         look it up in the schema of the user or the current one
    */
   static boolean lookedUpWithoutSchema(String dialect)
   {
      return !dialect.startsWith(DBConstants.DB_DIALECT_ORACLE) && !dialect.startsWith(DBConstants.DB_DIALECT_DB2);
   }

   /**
    * @return the qualified name of a table that the lookup of Infinispan finds for the given one outside the
    *         database and the schema of the connection, <code>null</code> when it finds none
    */
   static String findElsewhere(Connection connection, String table) throws SQLException
   {
      String name = table.replaceAll("^[\"`](.*)[\"`]$", "$1");
      String schema = null;
      int dot = name.indexOf('.');
      if (dot > 0)
      {
         // Infinispan reads the schema from the prefix of the table name
         schema = name.substring(0, dot);
         name = name.substring(dot + 1);
      }
      String catalog = connection.getCatalog();
      String connectionSchema = getSchema(connection);
      // The lookup of Infinispan: no catalog, the name as a pattern
      try (ResultSet found = connection.getMetaData().getTables(null, schema, name, new String[]{"TABLE"}))
      {
         while (found.next())
         {
            String foundCatalog = found.getString("TABLE_CAT");
            String foundSchema = found.getString("TABLE_SCHEM");
            String foundName = found.getString("TABLE_NAME");
            boolean here = sameOrUnknown(foundCatalog, catalog) && sameOrUnknown(foundSchema, connectionSchema);
            if (!here || !name.equalsIgnoreCase(foundName))
            {
               return (foundCatalog == null ? "" : foundCatalog + ".") + (foundSchema == null ? "" : foundSchema + ".")
                  + foundName;
            }
         }
      }
      return null;
   }

   /**
    * @return whether two names of a catalog or a schema are the same, a name the driver does not give matching any;
    *         MySQL may give a name in another case than the one it was created with
    */
   private static boolean sameOrUnknown(String found, String expected)
   {
      return found == null || expected == null || found.equalsIgnoreCase(expected);
   }

   /**
    * Whether the lock table exists without the segment column of the lock store.
    */
   private boolean isLegacy(Connection connection, String lockTable) throws SQLException
   {
      if (!tableExists(connection, lockTable))
      {
         return false;
      }
      try (Statement statement = connection.createStatement();
           ResultSet empty = statement.executeQuery("SELECT * FROM " + lockTable + " WHERE 1 = 0"))
      {
         ResultSetMetaData metaData = empty.getMetaData();
         for (int i = 1; i <= metaData.getColumnCount(); i++)
         {
            if (metaData.getColumnName(i).equalsIgnoreCase(table.segmentColumnName()))
            {
               return false;
            }
         }
         return true;
      }
   }

   /**
    * Renames the lock table to the claim table, which only one node can do, and adds the reserved row. The rename
    * is run by the tests on HSQLDB, MySQL and PostgreSQL only: the SQL Server, Sybase and DB2 forms follow each engine's
    * documented statement, and Oracle takes the ALTER TABLE form; none of them is run by a test.
    */
   private boolean claim(Connection connection, String lockTable, String claimTable) throws SQLException
   {
      String dialect = DialectDetecter.detect(connection.getMetaData());
      String rename;
      if (dialect.startsWith(DBConstants.DB_DIALECT_MSSQL) || dialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
      {
         rename = "EXEC sp_rename '" + unquote(lockTable) + "', '" + unquote(claimTable) + "'";
      }
      else if (dialect.startsWith(DBConstants.DB_DIALECT_DB2))
      {
         rename = "RENAME TABLE " + lockTable + " TO " + claimTable;
      }
      else
      {
         rename = "ALTER TABLE " + lockTable + " RENAME TO " + claimTable;
      }
      try (Statement statement = connection.createStatement())
      {
         statement.executeUpdate(rename);
      }
      catch (SQLException e)
      {
         if (isLegacy(connection, lockTable) && !tableExists(connection, claimTable))
         {
            throw e;
         }
         LOG.debug("The lock table {} was claimed by another node", lockTable, e);
         return false;
      }
      insertClaim(connection, claimTable);
      return true;
   }

   private void insertClaim(Connection connection, String claimTable) throws SQLException
   {
      long now = System.currentTimeMillis();
      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + claimTable + " ("
         + table.idColumnName() + ", " + table.dataColumnName() + ", " + table.timestampColumnName()
         + ") VALUES (?, ?, ?)"))
      {
         insert.setString(1, CLAIM_ID);
         insert.setBytes(2, node.getBytes(StandardCharsets.UTF_8));
         insert.setLong(3, now);
         insert.executeUpdate();
      }
      heartbeat = now;
   }

   /**
    * Adds the reserved row to a claim table left without one, unless another node added it meanwhile.
    */
   private boolean insertClaimUnlessPresent(Connection connection, String claimTable) throws SQLException
   {
      try
      {
         insertClaim(connection, claimTable);
         return true;
      }
      catch (SQLException e)
      {
         if (readHeartbeat(connection, claimTable) == null)
         {
            throw e;
         }
         LOG.debug("The claim {} was taken by another node", claimTable, e);
         return false;
      }
   }

   /**
    * @return the heartbeat of the reserved row, null when the claim table has no reserved row, {@link #GONE} when
    *         the claim table was dropped meanwhile
    */
   private Long readHeartbeat(Connection connection, String claimTable) throws SQLException
   {
      try (PreparedStatement select = connection.prepareStatement("SELECT " + table.timestampColumnName() + " FROM "
         + claimTable + " WHERE " + table.idColumnName() + " = ?"))
      {
         select.setString(1, CLAIM_ID);
         try (ResultSet row = select.executeQuery())
         {
            return row.next() ? row.getLong(1) : null;
         }
      }
      catch (SQLException e)
      {
         if (tableExists(connection, claimTable))
         {
            throw e;
         }
         return GONE;
      }
   }

   /**
    * Takes the claim over, with an update that only the first node that read this heartbeat can do.
    */
   private boolean takeOver(Connection connection, String claimTable, long claimed)
   {
      LOG.info("Taking over the upgrade of the lock store of the workspace {}, whose heartbeat is {} ms old",
         workspace.getUniqueName(), System.currentTimeMillis() - claimed);
      return beat(connection, claimTable, claimed);
   }

   /**
    * Writes a new heartbeat, provided the reserved row holds the given one.
    */
   private boolean beat(Connection connection, String claimTable, long expected)
   {
      long now = Math.max(System.currentTimeMillis(), expected + 1);
      try (PreparedStatement update = connection.prepareStatement("UPDATE " + claimTable + " SET "
         + table.dataColumnName() + " = ?, " + table.timestampColumnName() + " = ? WHERE " + table.idColumnName()
         + " = ? AND " + table.timestampColumnName() + " = ?"))
      {
         update.setBytes(1, node.getBytes(StandardCharsets.UTF_8));
         update.setLong(2, now);
         update.setString(3, CLAIM_ID);
         update.setLong(4, expected);
         if (update.executeUpdate() != 1)
         {
            return false;
         }
      }
      catch (SQLException e)
      {
         LOG.debug("The claim {} is gone", claimTable, e);
         return false;
      }
      heartbeat = now;
      return true;
   }

   /**
    * Releases the locks of the nodes that hold lock properties, in batches, then drops the claim table.
    *
    * @return false if another node took the claim over meanwhile
    */
   private boolean upgrade(String claimTable) throws SQLException, RepositoryException, RepositoryConfigurationException
   {
      List<String> lockedNodes = getNodesWithLockProperties();
      int released = 0;
      // The claim is renewed before each batch and before the drop: a node another node took the claim over from
      // releases nothing more and leaves the claim table to that node
      for (int i = 0; holdsClaim(claimTable); i += batchSize)
      {
         if (i >= lockedNodes.size())
         {
            try (Connection connection = lockDataSource.getConnection();
                 Statement statement = connection.createStatement())
            {
               statement.executeUpdate("DROP TABLE " + claimTable);
            }
            LOG.info("Upgraded the lock store of the workspace {}: the locks of {} nodes released, the table {} dropped",
               workspace.getUniqueName(), released, claimTable);
            return true;
         }
         List<String> batch = new ArrayList<>(lockedNodes.subList(i, Math.min(i + batchSize, lockedNodes.size())));
         // A node locked in the new store meanwhile, by a node already started, keeps its lock
         batch.removeAll(getNodesLockedInTheNewStore());
         if (!batch.isEmpty())
         {
            released += releaser.release(batch);
         }
      }
      LOG.info("Another node took over the upgrade of the lock store of the workspace {}", workspace.getUniqueName());
      return false;
   }

   /**
    * The nodes locked in the new store, none while its table does not exist; a failed lookup or read stops the upgrade.
    */
   private Set<String> getNodesLockedInTheNewStore() throws SQLException
   {
      try (Connection connection = lockDataSource.getConnection())
      {
         if (!tableExists(connection, tables.getTableName("L")))
         {
            return Set.of();
         }
      }
      return tables.getLockedNodesIds();
   }

   /**
    * Whether a table exists, looked up in the database metadata. A failed lookup throws: a check that answers "absent"
    * on any error, as {@link JDBCUtils#tableExists(String, Connection)} does, would let a waiting node start its lock
    * store while another node upgrades the old table, or release the locks the new store holds.
    *
    * <p>
    * The lookup is narrowed to the connection's catalog and schema, where the statements that follow it read the
    * table unqualified: a table of the same name in another schema the account can see, another platform's on the same
    * database, is not this workspace's. A connection that cannot name its schema, from a pool written before JDBC 4.1,
    * is looked up in every schema: on a database with schemas (PostgreSQL, Oracle, SQL Server) such a table then reads
    * as this workspace's. MySQL, whose schema is the catalog, is not concerned.
    *
    * @param table the table's name, quoted as {@link ISPNLockTableHandler} quotes it, which keeps its case; a database
    *          that folds names stores it upper or lower case, so the three are looked up
    */
   static boolean tableExists(Connection connection, String table) throws SQLException
   {
      String name = table.replaceAll("^[\"`](.*)[\"`]$", "$1");
      DatabaseMetaData metaData = connection.getMetaData();
      String escape = metaData.getSearchStringEscape();
      String schema = getSchema(connection);
      String schemaPattern = schema == null ? null : escape(schema, escape);
      for (String candidate : new LinkedHashSet<>(List.of(name, name.toUpperCase(Locale.ROOT),
         name.toLowerCase(Locale.ROOT))))
      {
         try (ResultSet found = metaData.getTables(connection.getCatalog(), schemaPattern, escape(candidate, escape),
            null))
         {
            if (found.next())
            {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * @return the schema of the connection, or <code>null</code> when it cannot name it: a connection pool written
    *         before JDBC 4.1, such as Commons DBCP 1, does not implement {@link Connection#getSchema()}
    */
   private static String getSchema(Connection connection) throws SQLException
   {
      try
      {
         return connection.getSchema();
      }
      catch (AbstractMethodError | SQLFeatureNotSupportedException e) // NOSONAR
      {
         return null;
      }
   }

   /**
    * @return the name as a metadata search pattern that matches it only
    */
   private static String escape(String name, String escape)
   {
      return escape == null ? name : name.replace(escape, escape + escape).replace("_", escape + "_")
         .replace("%", escape + "%");
   }

   /**
    * Renews the heartbeat of the claim of this node.
    *
    * @return false if another node took the claim over
    */
   private boolean holdsClaim(String claimTable) throws SQLException
   {
      try (Connection connection = lockDataSource.getConnection())
      {
         return beat(connection, claimTable, heartbeat);
      }
   }

   /**
    * The identifiers of the nodes of the workspace that hold a lock property, read in the item table.
    */
   private List<String> getNodesWithLockProperties() throws SQLException, RepositoryConfigurationException
   {
      boolean multiDatabase = DBInitializerHelper.getDatabaseType(workspace).isMultiDatabase();
      String containerName = workspace.getName();
      List<String> nodes = new ArrayList<>();
      try (Connection connection = itemDataSource.getConnection();
           PreparedStatement select = connection.prepareStatement("SELECT DISTINCT PARENT_ID FROM "
              + DBInitializerHelper.getItemTableName(workspace) + " WHERE I_CLASS = 2 AND (NAME = ? OR NAME = ?)"
              + (multiDatabase ? "" : " AND CONTAINER_NAME = ?")))
      {
         select.setString(1, Constants.JCR_LOCKOWNER.getAsString());
         select.setString(2, Constants.JCR_LOCKISDEEP.getAsString());
         if (!multiDatabase)
         {
            select.setString(3, containerName);
         }
         try (ResultSet rows = select.executeQuery())
         {
            while (rows.next())
            {
               String parentId = rows.getString(1);
               nodes.add(multiDatabase ? parentId : parentId.substring(containerName.length()));
            }
         }
      }
      return nodes;
   }

   private static String unquote(String name)
   {
      return name.substring(1, name.length() - 1);
   }
}
