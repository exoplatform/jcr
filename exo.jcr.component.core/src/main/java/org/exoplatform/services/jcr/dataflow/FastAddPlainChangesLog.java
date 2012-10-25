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
package org.exoplatform.services.jcr.dataflow;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Designed to be fast on item addition. Avoided CPU consuming in creation
 * unnecessary maps. Thus some methods will throw {@link UnsupportedOperationException}
 * to avoid inconsistent result. Is good at persistent level and totally unusable
 * in session.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 */
public class FastAddPlainChangesLog extends PlainChangesLogImpl
{

   /**
    * Serialization needs.
    */
   public FastAddPlainChangesLog()
   {
   }

   /**
    * FastAddPlainChangesLog constructor.
    */
   private FastAddPlainChangesLog(String sessionId, int eventType, String pairId, ExtendedSession session)
   {
      super(new ArrayList<ItemState>(), sessionId, eventType, pairId, session);
   }

   /**
    * Factory method.
    */
   public static PlainChangesLog getInstance(PlainChangesLog originalLog)
   {
      if (originalLog.getSession() != null)
      {
         return new FastAddPlainChangesLog(originalLog.getSession().getId(), originalLog.getEventType(),
            originalLog.getPairId(), originalLog.getSession());
      }

      return new FastAddPlainChangesLog(originalLog.getSessionId(), originalLog.getEventType(),
         originalLog.getPairId(), null);
   }

   /**
    * {@inheritDoc}
    */
   public PlainChangesLog addAll(List<ItemState> changes)
   {
      items.addAll(changes);
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public PlainChangesLog add(ItemState change)
   {
      items.add(change);
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public void remove(ItemState item)
   {
      items.add(item);
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(String rootIdentifier)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public int getLastChildOrderNumber(String rootIdentifier)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public void remove(QPath rootPath)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public Collection<ItemState> getLastChildrenStates(ItemData rootData, boolean forNodes)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState getLastState(ItemData item, boolean forNode)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public List<ItemState> getChildrenChanges(String rootIdentifier, boolean forNodes)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState getItemState(String itemIdentifier, int state)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState getItemState(String itemIdentifier)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState getItemState(QPath itemPath)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState getItemState(NodeData parentData, QPathEntry name, ItemType itemType) throws IllegalPathException
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public List<ItemState> getDescendantsChanges(QPath rootPath)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public List<ItemState> getItemStates(String itemIdentifier)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public Collection<ItemState> getLastModifyStates(NodeData rootData)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public ItemState findItemState(String id, Boolean isPersisted, int... states) throws IllegalPathException
   {
      throw new UnsupportedOperationException("Method is not supported");
   }
}