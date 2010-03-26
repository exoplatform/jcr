/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ReplicationLockTest.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class ReplicationLockTest extends BaseReplicationTest
{

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.ReplicationLockTest");

   public void testLock() throws Exception
   {
      Node nodeLocked = root.addNode("Node Locked");
      nodeLocked.setProperty("jcr:data", "node data");
      nodeLocked.addMixin("mix:lockable");
      session.save();

      Thread.sleep(4 * 1000);

      Node destNodeLocked = root2.getNode("Node Locked");
      assertEquals("node data", destNodeLocked.getProperty("jcr:data").getString());
      assertEquals("mix:lockable", destNodeLocked.getMixinNodeTypes()[0].getName());
      assertEquals(false, destNodeLocked.isLocked());

      Lock lock = nodeLocked.lock(false, false);
      session.save();

      Thread.sleep(8 * 1000);

      assertEquals(true, destNodeLocked.isLocked());

      try
      {
         destNodeLocked.setProperty("jcr:data", "dd");
         session2.save();
         fail("Errore: Node is not locked");
      }
      catch (LockException e)
      {
         // ok
      }

      // unlock source node
      nodeLocked.unlock();

      Thread.sleep(8 * 1000);

      assertEquals(false, destNodeLocked.isLocked());

      // TODO
      // unlock destination node
      // lock = nodeLocked.lock(false, false);
      // session.save();
      //
      // Thread.sleep(4 * 1000);
      //
      // assertEquals(true, destNodeLocked.isLocked());
      //    
      // destNodeLocked.unlock();
      //
      // Thread.sleep(4 * 1000);
      //
      // assertEquals(false, nodeLocked.isLocked());
   }

   public void test10Lock() throws Exception
   {
      for (int i = 0; i < 10; i++)
      {
         log.info("Lock node #" + i + " ...");
         String lockName = "Node Locked";

         Node nodeLocked = root.addNode(lockName + i);
         nodeLocked.setProperty("jcr:data", "node data");
         nodeLocked.addMixin("mix:lockable");
         session.save();

         Thread.sleep(2 * 1000);

         Node destNodeLocked = root2.getNode(lockName + i);
         assertEquals("node data", destNodeLocked.getProperty("jcr:data").getString());
         assertEquals("mix:lockable", destNodeLocked.getMixinNodeTypes()[0].getName());
         assertEquals(false, destNodeLocked.isLocked());

         log.info("Set lock");
         Lock lock = nodeLocked.lock(false, false);
         session.save();

         Thread.sleep(2 * 1000);

         assertEquals(true, destNodeLocked.isLocked());

         try
         {
            destNodeLocked.setProperty("jcr:data", "dd");
            session2.save();
            fail("Errore: Node is not locked");
         }
         catch (LockException e)
         {
            // ok
         }

         log.info("Set unlock");
         // unlock source node
         nodeLocked.unlock();

         Thread.sleep(2 * 1000);

         assertEquals(false, destNodeLocked.isLocked());
      }
   }

}
