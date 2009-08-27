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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import java.io.File;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 16.02.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestJCRSerislizationLock.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestJCRSerislizationLock
   extends JcrImplSerializationBaseTest
{

   public void test10Lock() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      for (int i = 0; i < 10; i++)
      {
         log.info("Lock node #" + i + " ...");
         String lockName = "Node Locked";

         Node nodeLocked = root.addNode(lockName + i);
         nodeLocked.setProperty("jcr:data", "node data");
         nodeLocked.addMixin("mix:lockable");
         session.save();

         log.info("Set lock");
         Lock lock = nodeLocked.lock(false, false);
         session.save();

         log.info("Set unlock");
         nodeLocked.unlock();
      }

      List<TransactionChangesLog> srcLog = pl.pushChanges();

      File jcrfile = super.serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
   }
}
