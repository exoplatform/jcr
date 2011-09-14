/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMultipleListenersNotifying.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMultipleListenersNotifying extends TestCase
{

   class TestDataManager extends WorkspacePersistentDataManager
   {
      public TestDataManager()
      {
         super(null, new SystemDataContainerHolder(null));
      }

      public void save(ItemStateChangesLog changes) throws InvalidItemStateException, UnsupportedOperationException,
         RepositoryException
      {
         super.save(new ChangesLogWrapper(changes));
      }

      public ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType, boolean createNullItemData)
         throws RepositoryException
      {
         return null;
      }
   }

   class Counter
   {
      int count = 0;

      synchronized public void increment()
      {
         count++;
      }

      public int getCount()
      {
         return count;
      }

      public void reset()
      {
         count = 0;
      }
   }

   class TestTxAwareListener implements ItemsPersistenceListener
   {
      boolean isTXAware;

      Counter count;

      public TestTxAwareListener(boolean isTXAware, Counter count)
      {
         this.isTXAware = isTXAware;
         this.count = count;
      }

      public boolean isTXAware()
      {
         return isTXAware;
      }

      public void onSaveItems(ItemStateChangesLog itemStates)
      {
         count.increment();
      }
   }

   public void testMultipleListenersNotify() throws Exception
   {
      int listTransCount = 27;
      int listNotTransCount = 10;
      TestDataManager mgr = new TestDataManager();

      Counter countTrans = new Counter();
      Counter countNotTrans = new Counter();
      // set listeners
      for (int i = 0; i < listTransCount; i++)
      {
         mgr.addItemPersistenceListener(new TestTxAwareListener(true, countTrans));
      }
      for (int i = 0; i < listNotTransCount; i++)
      {
         mgr.addItemPersistenceListener(new TestTxAwareListener(false, countNotTrans));
      }

      mgr.notifySaveItems(null, true);
      assertEquals(listTransCount, countTrans.getCount());
      assertEquals(0, countNotTrans.getCount());

      countTrans.reset();
      countNotTrans.reset();

      mgr.notifySaveItems(null, false);
      assertEquals(0, countTrans.getCount());
      assertEquals(listNotTransCount, countNotTrans.getCount());
   }
}
