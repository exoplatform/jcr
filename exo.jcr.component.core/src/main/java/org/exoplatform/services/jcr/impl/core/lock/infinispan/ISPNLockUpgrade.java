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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
 * run again.
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

   private long heartbeat;

   /**
    * @param workspace the workspace
    * @param lockDataSource the data source of the lock table
    * @param itemDataSource the data source of the items of the workspace
    * @param table the table of the lock store
    * @param releaser the releaser of the locks of the nodes
    */
   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser)
   {
      this(workspace, lockDataSource, itemDataSource, table, releaser, STALE_AFTER, POLL_INTERVAL);
   }

   ISPNLockUpgrade(WorkspaceEntry workspace, DataSource lockDataSource, DataSource itemDataSource,
      TableManipulationConfiguration table, LockReleaser releaser, long staleAfter, long pollInterval)
   {
      this.workspace = workspace;
      this.tables = new ISPNLockTableHandler(workspace, lockDataSource);
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
         while (true)
         {
            try (Connection connection = lockDataSource.getConnection())
            {
               // A claim is awaited first: a node that lost the race to the claim reads it here
               if (!JDBCUtils.tableExists(claimTable, connection))
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
    * Whether the lock table exists without the segment column of the lock store.
    */
   private boolean isLegacy(Connection connection, String lockTable) throws SQLException
   {
      if (!JDBCUtils.tableExists(lockTable, connection))
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
    * Renames the lock table to the claim table, which only one node can do, and adds the reserved row.
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
         if (isLegacy(connection, lockTable) && !JDBCUtils.tableExists(claimTable, connection))
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
         if (JDBCUtils.tableExists(claimTable, connection))
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
      Set<String> newLocks = tables.tableExists() ? tables.getLockedNodesIds() : Set.of();
      lockedNodes.removeAll(newLocks);
      int released = 0;
      for (int i = 0; i < lockedNodes.size(); i += BATCH_SIZE)
      {
         released += releaser.release(lockedNodes.subList(i, Math.min(i + BATCH_SIZE, lockedNodes.size())));
         try (Connection connection = lockDataSource.getConnection())
         {
            if (!beat(connection, claimTable, heartbeat))
            {
               LOG.info("Another node took over the upgrade of the lock store of the workspace {}",
                  workspace.getUniqueName());
               return false;
            }
         }
      }
      try (Connection connection = lockDataSource.getConnection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DROP TABLE " + claimTable);
      }
      LOG.info("Upgraded the lock store of the workspace {}: the locks of {} nodes released, the table {} dropped",
         workspace.getUniqueName(), released, claimTable);
      return true;
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
