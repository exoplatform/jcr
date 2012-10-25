/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;

import java.io.File;

import javax.jcr.Node;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TestBackRestoreQuota.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestBackRestoreQuota extends AbstractQuotaManagerTest
{

   /**
    * Backup operation.
    */
   public void testBackupRestoreClean() throws Exception
   {
      assertWorkspaceSize(wsQuotaManager);

      File tempDir = new File("target/temp");

      Node testRoot = root.addNode("testRoot");

      wsQuotaManager.setNodeQuota("/testRoot/test1", 1000, false);
      wsQuotaManager.setNodeQuota("/testRoot/test2", 1000, false);
      wsQuotaManager.setGroupOfNodesQuota("/testRoot/*", 2000, true);

      testRoot.addNode("test1").addNode("content");
      testRoot.addNode("test2").addNode("content").addNode("content");
      root.save();

      assertNodeDataSize(wsQuotaManager, "/testRoot/test1");
      assertNodeDataSize(wsQuotaManager, "/testRoot/test2");

      wsQuotaManager.removeNodeQuota("/testRoot/test2");

      wsQuotaManager.suspend();
      assertTrue(wsQuotaManager.isSuspended());

      long repDataSize = dbQuotaManager.getRepositoryDataSize();
      long wsDataSize = wsQuotaManager.getWorkspaceDataSize();
      long node1DataSize = wsQuotaManager.getNodeDataSize("/testRoot/test1");
      long node2DataSize = wsQuotaManager.getNodeDataSize("/testRoot/test2");
      long node1Quota = wsQuotaManager.getNodeQuota("/testRoot/test1");
      long node2Quota = wsQuotaManager.getNodeQuota("/testRoot/test2");

      assertEquals(node1Quota, 1000);
      assertEquals(node2Quota, 2000);

      wsQuotaManager.backup(tempDir);

      DataRestoreContext context =
         new DataRestoreContext(new String[]{DataRestoreContext.STORAGE_DIR}, new Object[]{tempDir});
      DataRestore restorer = wsQuotaManager.getDataRestorer(context);

      restorer.clean();

      assertEquals(dbQuotaManager.getRepositoryDataSize(), repDataSize - wsDataSize);

      try
      {
         wsQuotaManager.getWorkspaceQuota();
         fail("Quota should be unknown after clean");
      }
      catch (UnknownQuotaLimitException e)
      {
      }

      try
      {
         wsQuotaManager.getWorkspaceDataSize();
         fail("Data size should be unknown after clean");
      }
      catch (UnknownDataSizeException e)
      {
      }

      assertTrue(dataSizeShouldNotExists(wsQuotaManager, "/testRoot/test1"));
      assertTrue(dataSizeShouldNotExists(wsQuotaManager, "/testRoot/test2"));
      assertTrue(quotaShouldNotExists(wsQuotaManager, "/testRoot/test1"));
      assertTrue(quotaShouldNotExists(wsQuotaManager, "/testRoot/test2"));

      restorer.rollback();

      assertEquals(dbQuotaManager.getRepositoryDataSize(), repDataSize);
      assertEquals(wsDataSize, wsQuotaManager.getWorkspaceDataSize());
      assertEquals(node1DataSize, wsQuotaManager.getNodeDataSize("/testRoot/test1"));
      assertEquals(node2DataSize, wsQuotaManager.getNodeDataSize("/testRoot/test2"));
      assertEquals(node1Quota, wsQuotaManager.getNodeQuota("/testRoot/test1"));
      assertEquals(node2Quota, wsQuotaManager.getNodeQuota("/testRoot/test2"));

      restorer.clean();
      restorer.restore();

      assertEquals(dbQuotaManager.getRepositoryDataSize(), repDataSize);
      assertEquals(wsDataSize, wsQuotaManager.getWorkspaceDataSize());
      assertEquals(node1DataSize, wsQuotaManager.getNodeDataSize("/testRoot/test1"));
      assertEquals(node2DataSize, wsQuotaManager.getNodeDataSize("/testRoot/test2"));
      assertEquals(node1Quota, wsQuotaManager.getNodeQuota("/testRoot/test1"));
      assertEquals(node2Quota, wsQuotaManager.getNodeQuota("/testRoot/test2"));

      restorer.commit();
      restorer.close();

      wsQuotaManager.resume();
      assertFalse(wsQuotaManager.isSuspended());

      wsQuotaManager.removeGroupOfNodesQuota("/testRoot/*");
      wsQuotaManager.removeNodeQuota("/testRoot/test1");
      wsQuotaManager.removeNodeQuota("/testRoot/test2");
   }

}
