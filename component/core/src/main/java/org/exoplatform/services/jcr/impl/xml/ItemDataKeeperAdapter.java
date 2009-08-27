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
package org.exoplatform.services.jcr.impl.xml;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;

/**
 * Created by The eXo Platform SAS. ItemDataKeeper for SessionDataManager. Used by XML import.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class ItemDataKeeperAdapter
   implements ItemDataKeeper
{
   /**
    * Internal data manager.
    */
   private final SessionDataManager sessionDataManager;

   /**
    * Adapter from SessionDataManager to ItemDataKeeper.
    * 
    * @param sessionDataManager
    *          - Data manager.
    */
   public ItemDataKeeperAdapter(SessionDataManager sessionDataManager)
   {
      super();
      this.sessionDataManager = sessionDataManager;
   }

   /**
    * {@inheritDoc}
    */

   public void save(ItemStateChangesLog changes) throws InvalidItemStateException, UnsupportedOperationException,
            RepositoryException
   {
      for (ItemState itemState : changes.getAllStates())
      {
         if (itemState.isAdded())
            sessionDataManager.update(itemState, false);
         else if (itemState.isDeleted())
            sessionDataManager.delete(itemState.getData(), itemState.getAncestorToSave());
      }
   }
}
