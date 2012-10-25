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

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Wraps information of changed data size of particular save. First, is put into pending changes
 * and after save is performed being moved into changes log.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ChangesItem.java 34360 2009-07-22 23:58:59Z tolusha $
 */
class ChangesItem implements Externalizable
{
   /**
    * ChangesItem constructor.
    */
   public ChangesItem()
   {
   }

   /**
    * Contains calculated workspace data size changes of particular save.
    */
   private long workspaceChangedSize;

   /**
    * Contains calculated node data size changes of particular save.
    * Represents {@link Map} with absolute node path as key and changed node
    * data size as value respectively.
    */
   private Map<String, Long> calculatedChangedNodesSize = new HashMap<String, Long>();

   /**
    * Set of absolute nodes paths for which changes were made but changed size is unknown. Most
    * famous case when {@link WorkspaceDataContainer#TRIGGER_EVENTS_FOR_DESCENDENTS_ON_RENAME} is
    * set to false and move operation is performed.
    */
   private Set<String> unknownChangedNodesSize = new HashSet<String>();

   /**
    * Collects node paths for which data size updating must be performed asynchronously.
    */
   private Set<String> asyncUpdate = new HashSet<String>();

   /**
    * Getter for {@link #workspaceChangedSize}.
    */
   public long getWorkspaceChangedSize()
   {
      return workspaceChangedSize;
   }

   /**
    * Updates {@link #workspaceChangedSize}.
    */
   public void updateWorkspaceChangedSize(long delta)
   {
      workspaceChangedSize += delta;
   }

   /**
    * Returns node data changed size if exists or zero otherwise.
    */
   public long getNodeChangedSize(String nodePath)
   {
      Long delta = calculatedChangedNodesSize.get(nodePath);
      return delta == null ? 0 : delta;
   }

   /**
    * Getter for {@link #calculatedChangedNodesSize}.
    */
   public Map<String, Long> getAllNodesCalculatedChangedSize()
   {
      return calculatedChangedNodesSize;
   }

   /**
    * Getter for {@link #unknownChangedNodesSize}.
    */
   public Set<String> getAllNodesUnknownChangedSize()
   {
      return unknownChangedNodesSize;
   }

   /**
    * Updates {@link #calculatedChangedNodesSize} for particular
    * node path.
    */
   public void updateNodeChangedSize(String nodePath, long delta)
   {
      Long oldDelta = calculatedChangedNodesSize.get(nodePath);
      Long newDelta = delta + (oldDelta != null ? oldDelta : 0);

      calculatedChangedNodesSize.put(nodePath, newDelta);
   }

   /**
    * Adds new node absolute path for {@link #unknownChangedNodesSize} collection.
    */
   public void addPathWithUnknownChangedSize(String nodePath)
   {
      unknownChangedNodesSize.add(nodePath);
   }

   /**
    * Adds new node absolute path for {@link #asyncUpdate} collection.
    */
   public void addPathWithAsyncUpdate(String nodePath)
   {
      asyncUpdate.add(nodePath);
   }

   /**
    * Merges current changes with new one.
    */
   public void merge(ChangesItem changesItem)
   {
      workspaceChangedSize += changesItem.getWorkspaceChangedSize();

      for (Entry<String, Long> changesEntry : changesItem.calculatedChangedNodesSize.entrySet())
      {
         String nodePath = changesEntry.getKey();
         Long currentDelta = changesEntry.getValue();

         Long oldDelta = calculatedChangedNodesSize.get(nodePath);
         Long newDelta = currentDelta + (oldDelta == null ? 0 : oldDelta);

         calculatedChangedNodesSize.put(nodePath, newDelta);
      }

      for (String path : changesItem.unknownChangedNodesSize)
      {
         unknownChangedNodesSize.add(path);
      }

      for (String path : changesItem.asyncUpdate)
      {
         asyncUpdate.add(path);
      }
   }

   /**
    * Checks if there is any changes.
    */
   public boolean isEmpty()
   {
      return workspaceChangedSize == 0 && calculatedChangedNodesSize.isEmpty() && unknownChangedNodesSize.isEmpty();
   }

   /**
    * Leave in {@link ChangesItem} only changes being apply asynchronously
    * and return ones to apply instantly.
    */
   public ChangesItem extractSyncChanges()
   {
      ChangesItem syncChangesItem = new ChangesItem();

      Iterator<String> iter = calculatedChangedNodesSize.keySet().iterator();
      while (iter.hasNext() && !asyncUpdate.isEmpty())
      {
         String nodePath = iter.next();

         if (!asyncUpdate.contains(nodePath))
         {
            Long chanagedSize = calculatedChangedNodesSize.get(nodePath);
            syncChangesItem.calculatedChangedNodesSize.put(nodePath, chanagedSize);
            syncChangesItem.workspaceChangedSize += chanagedSize;

            this.asyncUpdate.remove(nodePath);
            this.workspaceChangedSize -= chanagedSize;

            iter.remove();
         }
      }

      return syncChangesItem;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeLong(workspaceChangedSize);

      out.writeInt(calculatedChangedNodesSize.size());
      for (Entry<String, Long> entry : calculatedChangedNodesSize.entrySet())
      {
         writeString(out, entry.getKey());
         out.writeLong(entry.getValue());
      }

      out.writeInt(unknownChangedNodesSize.size());
      for (String path : unknownChangedNodesSize)
      {
         writeString(out, path);
      }

      out.writeInt(asyncUpdate.size());
      for (String path : asyncUpdate)
      {
         writeString(out, path);
      }
   }

   private void writeString(ObjectOutput out, String str) throws IOException
   {
      byte[] data = str.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(data.length);
      out.write(data);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      this.workspaceChangedSize = in.readLong();

      int size = in.readInt();
      this.calculatedChangedNodesSize = new HashMap<String, Long>(size);
      for (int i = 0; i < size; i++)
      {
         String nodePath = readString(in);
         Long delta = in.readLong();

         calculatedChangedNodesSize.put(nodePath, delta);
      }

      size = in.readInt();
      this.unknownChangedNodesSize = new HashSet<String>(size);
      for (int i = 0; i < size; i++)
      {
         String nodePath = readString(in);
         unknownChangedNodesSize.add(nodePath);
      }

      size = in.readInt();
      this.asyncUpdate = new HashSet<String>(size);
      for (int i = 0; i < size; i++)
      {
         String nodePath = readString(in);
         asyncUpdate.add(nodePath);
      }
   }

   private String readString(ObjectInput in) throws IOException
   {
      byte[] data = new byte[in.readInt()];
      in.readFully(data);

      return new String(data, Constants.DEFAULT_ENCODING);
   }
}
