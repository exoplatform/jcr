/*
 * Copyright (C) 2010 eXo Platform SAS.
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
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.LockData;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.jdbc.LockJDBCConnection;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.jdbc.LockJDBCContainer;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestLockPerstistentDataManager.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestLockPerstistentDataManager extends JcrImplBaseTest
{
   public void testAddLockData() throws RepositoryException
   {
      LockJDBCContainer dataManager = new LockJDBCContainer("jdbcjcrtest", "ws");
      LockJDBCConnection connection = null;
      try
      {
         // get connection to lock storage
         connection = dataManager.openConnection();
         // put lock data
         connection.addLockData(new LockData("identifier", "hash", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();
         // acquire new connection
         connection = dataManager.openConnection();
         // get lock data
         LockData lockData = connection.getLockData("identifier");
         // asserts
         assertTrue("Lock data should not be null", lockData != null);
         assertEquals("identifier", lockData.getNodeIdentifier());
         assertEquals("hash", lockData.getTokenHash());
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

   public void testRemoveLockData() throws RepositoryException
   {
      LockJDBCContainer dataManager = new LockJDBCContainer("jdbcjcrtest", "ws");
      LockJDBCConnection connection = null;
      try
      {
         // get connection to lock storage
         connection = dataManager.openConnection();
         // put lock data
         connection.addLockData(new LockData("identifier2", "hash", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();
         // acquire new connection
         connection = dataManager.openConnection();
         // get lock data
         LockData lockData = connection.getLockData("identifier2");
         // asserts
         assertTrue("Lock data should not be null", lockData != null);
         // remove lock data
         connection.removeLockData("identifier2");
         // commit also closes connection
         connection.commit();
         // acquire new connection     
         connection = dataManager.openConnection();
         lockData = connection.getLockData("identifier2");
         // asserts
         assertTrue("Lock data should be null", lockData == null);
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

   public void testRefreshLockData() throws RepositoryException
   {
      LockJDBCContainer dataManager = new LockJDBCContainer("jdbcjcrtest", "ws");
      LockJDBCConnection connection = null;
      try
      {
         // get connection to lock storage
         connection = dataManager.openConnection();
         // put lock data
         connection.addLockData(new LockData("identifier3", "hash", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();
         // sleep
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
         }
         // acquire new connection
         connection = dataManager.openConnection();
         // get lock Data
         LockData lockData = connection.getLockData("identifier3");
         Long timeToDeathOriginal = lockData.getTimeToDeath();
         // refresh lock data
         connection.refreshLockData(new LockData("identifier3", "hash", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();
         // acquire new connection     
         connection = dataManager.openConnection();
         lockData = connection.getLockData("identifier3");
         Long timeToDeathNew = lockData.getTimeToDeath();
         // asserts
         assertTrue("Birthday should be refreshed", timeToDeathNew > timeToDeathOriginal);
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

   public void testgetLockedNodes() throws RepositoryException
   {
      LockJDBCContainer dataManager = new LockJDBCContainer("jdbcjcrtest", "test_workspace");
      LockJDBCContainer dataManagerAnotherWS = new LockJDBCContainer("jdbcjcrtest", "another_workspace");
      LockJDBCConnection connection = null;
      try
      {
         // get connection to lock storage
         connection = dataManager.openConnection();
         // put lock data
         connection.addLockData(new LockData("identifier1-listTest", "hash1", false, false, "owner", 100));
         connection.addLockData(new LockData("identifier2-listTest", "hash2", false, false, "owner", 100));
         connection.addLockData(new LockData("identifier3-listTest", "hash3", false, false, "owner", 100));
         connection.addLockData(new LockData("identifier4-listTest", "hash4", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();

         // Adding lock data to another workspace
         connection = dataManagerAnotherWS.openConnection();
         // this lock data is from another workspace and shouldn't be in result set
         connection.addLockData(new LockData("identifier1-listTest", "hash1", false, false, "owner", 100));
         connection.commit();
         // acquire new connection
         connection = dataManager.openConnection();
         // get set
         Set<String> identifiers = connection.getLockedNodes();
         assertEquals("Wrong size of result.", 4, identifiers.size());
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

   public void testAddLockDataTwice() throws RepositoryException
   {
      LockJDBCContainer dataManager = new LockJDBCContainer("jdbcjcrtest", "ws");
      LockJDBCConnection connection = null;
      try
      {
         // get connection to lock storage
         connection = dataManager.openConnection();
         // put lock data
         connection.addLockData(new LockData("identifier", "hash", false, false, "owner", 100));
         // commit also closes connection
         connection.commit();
         // acquire new connection
         connection = dataManager.openConnection();
         // put lock data with same identifier
         connection.addLockData(new LockData("identifier", "hash", false, false, "owner", 100));
         fail("exception expected!");
      }
      catch (LockException e)
      {
         // it's ok
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

}
