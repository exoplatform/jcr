/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;

import java.io.File;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.lock.Lock;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 16.02.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: TestJCRSerislizationLock.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class TestJCRSerislizationLock extends JcrImplSerializationBaseTest
{

   public void test10Lock() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      for (int i = 0; i < 10; i++)
      {
         String lockName = "Node Locked";

         Node nodeLocked = root.addNode(lockName + i);
         nodeLocked.setProperty("jcr:data", "node data");
         nodeLocked.addMixin("mix:lockable");
         session.save();

         Lock lock = nodeLocked.lock(false, false);
         session.save();

         nodeLocked.unlock();
      }

      List<TransactionChangesLog> srcLog = pl.pushChanges();

      File jcrfile = super.serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = super.deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
      {
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
      }
   }
}
