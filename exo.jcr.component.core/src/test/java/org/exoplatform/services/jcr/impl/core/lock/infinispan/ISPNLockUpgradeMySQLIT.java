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

import static org.junit.Assert.assertFalse;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.mysql.MySQLContainer;

import com.mysql.cj.jdbc.MysqlDataSource;

/**
 * The claim of the upgrade of a lock store on MySQL, in a container: {@code mysql:8.4}, or the image of the
 * jcr.it.mysql.image property. Skipped without Docker.
 */
public class ISPNLockUpgradeMySQLIT extends TestISPNLockUpgradeClaim
{
   private static MySQLContainer database;

   @BeforeClass
   public static void startDatabase()
   {
      Assume.assumeTrue("Docker is needed to run MySQL", DockerClientFactory.instance().isDockerAvailable());
      // The root account, which can create the database of another platform beside the test's
      database = new MySQLContainer(DockerImageName.parse(System.getProperty("jcr.it.mysql.image", "mysql:8.4")).asCompatibleSubstituteFor("mysql"))
                                                                                                                .withUsername("root");
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
      MysqlDataSource mysql = new MysqlDataSource();
      mysql.setURL(database.getJdbcUrl());
      mysql.setUser(database.getUsername());
      mysql.setPassword(database.getPassword());
      return mysql;
   }

   @Override
   protected String binaryType()
   {
      return "LONGBLOB";
   }

   /**
    * With nullDatabaseMeansCurrent=true on its URL, Connector/J reads the lookup of Infinispan in the database of the
    * URL only: the lock table of another database is not taken, and the workspace starts.
    */
   @Test
   public void testTheUrlThatNarrowsTheLookupToItsDatabaseStartsBesideAnotherDatabasesStore() throws Exception
   {
      createTheNewStoreOfAnotherSchema();
      MysqlDataSource narrowed = (MysqlDataSource)createDataSource();
      String url = database.getJdbcUrl();
      narrowed.setURL(url + (url.contains("?") ? "&" : "?") + "nullDatabaseMeansCurrent=true");

      assertFalse(upgrade(narrowed).run());
   }

   @Override
   protected void dropDatabase() throws SQLException
   {
      dropTables();
      // A MySQL schema is a database, which outlives the test
      execute("DROP DATABASE IF EXISTS " + OTHER_SCHEMA);
   }
}
