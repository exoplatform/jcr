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
package org.exoplatform.services.jcr.impl.dataflow.session;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;

import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.<br/> Responsible for managing session changes log. Relying on
 * fact that ItemData inside ItemState SHOULD be TransientItemData
 * 
 * @author Gennady Azarenkov
 * @version $Id: SessionChangesLog.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public final class SessionChangesLog extends PlainChangesLogImpl
{

   /**
    * Create empty ChangesLog.
    * 
    * @param sessionId
    */
   public SessionChangesLog(ExtendedSession session)
   {
      super(session);
   }

   /**
    * Create ChangesLog and populate with given items changes.
    * 
    * @param items
    * @param sessionId
    */
   public SessionChangesLog(List<ItemState> items, ExtendedSession session)
   {
      super(items, session);
      for (int i = 0, length = items.size(); i < length; i++)
      {
         ItemState change = items.get(i);
         addItem(change);
      }
   }

   /**
    * An example of use: transient changes of item added and removed in same session. These changes
    * must not fire events in observation.
    * 
    * @param identifier
    */
   public void eraseEventFire(String identifier)
   {
      ItemState item = getItemState(identifier);
      if (item != null)
      {
         item.eraseEventFire();
         Map<String, ItemState> children = lastChildPropertyStates.get(identifier);
         if (children != null)
         {
            // Call the method ItemState.eraseEventFire() on each properties
            for (ItemState child : children.values())
            {
               child.eraseEventFire();
            }
         }
         children = lastChildNodeStates.get(identifier);
         if (children != null)
         {
            // Recursively call the method eraseEventFire(String identifier) for each sub node
            for (ItemState child : children.values())
            {
               eraseEventFire(child.getData().getIdentifier());
            }
         }
      }
   }

   /**
    * Creates new changes log with rootPath and its descendants of this one and removes those
    * entries.
    * 
    * @param rootPath
    * @return ItemDataChangesLog
    */
   public PlainChangesLog pushLog(QPath rootPath)
   {
      // session instance is always present in SessionChangesLog
      PlainChangesLog cLog = new PlainChangesLogImpl(getDescendantsChanges(rootPath), session);
      if (rootPath.equals(Constants.ROOT_PATH))
      {
         clear();
      }
      else
      {
         remove(rootPath);
      }
      return cLog;
   }
}
