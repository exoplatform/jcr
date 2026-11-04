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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.postgresql.ds.PGSimpleDataSource;

/**
 * The claim of the upgrade of a lock store on PostgreSQL, in a container: {@code postgres:16}, or the image of the
 * jcr.it.postgresql.image property. Skipped without Docker.
 */
public class ISPNLockUpgradePostgreSQLIT extends TestISPNLockUpgradeClaim
{
   private static PostgreSQLContainer database;

   @BeforeClass
   public static void startDatabase()
   {
      Assume.assumeTrue("Docker is needed to run PostgreSQL", DockerClientFactory.instance().isDockerAvailable());
      database = new PostgreSQLContainer(DockerImageName.parse(System.getProperty("jcr.it.postgresql.image", "postgres:16")).asCompatibleSubstituteFor("postgres"));
      database.start();
   }

   @AfterClass
   public static void stopDatabase()
   {
      if (database != null)
      {
         database.stop();
      }
   }

   @Override
   protected DataSource createDataSource() throws SQLException
   {
      PGSimpleDataSource postgresql = new PGSimpleDataSource();
      postgresql.setURL(database.getJdbcUrl());
      postgresql.setUser(database.getUsername());
      postgresql.setPassword(database.getPassword());
      return postgresql;
   }

   @Override
   protected String binaryType()
   {
      return "BYTEA";
   }

   @Override
   protected void dropDatabase() throws SQLException
   {
      dropTables();
   }
}
