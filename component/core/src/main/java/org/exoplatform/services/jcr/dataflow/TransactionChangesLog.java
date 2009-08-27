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
package org.exoplatform.services.jcr.dataflow;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransactionChangesLog.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TransactionChangesLog
   implements CompositeChangesLog, Externalizable
{

   private static final long serialVersionUID = 4866736965040228027L;

   protected String systemId;

   protected List<PlainChangesLog> changesLogs;

   public TransactionChangesLog()
   {
      changesLogs = new ArrayList<PlainChangesLog>();
   }

   public TransactionChangesLog(PlainChangesLog changesLog)
   {
      changesLogs = new ArrayList<PlainChangesLog>();
      changesLogs.add(changesLog);
   }

   /**
    * {@inheritDoc}
    */
   public void addLog(PlainChangesLog log)
   {
      changesLogs.add(log);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.CompositeChangesLog#getLogIterator()
    */
   public ChangesLogIterator getLogIterator()
   {
      return new ChangesLogIterator(changesLogs);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemStateChangesLog#getAllStates()
    */
   public List<ItemState> getAllStates()
   {
      // TODO [PN] use a wrapping List/Iterator for all changes logs instead of
      // putting all logs
      // content into one list
      // will increase a performance of tx-related operations
      List<ItemState> states = new ArrayList<ItemState>();
      for (PlainChangesLog changesLog : changesLogs)
      {
         for (ItemState state : changesLog.getAllStates())
         {
            states.add(state);
         }
      }
      return states;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemStateChangesLog#getSize()
    */
   public int getSize()
   {
      int size = 0;
      for (PlainChangesLog changesLog : changesLogs)
      {
         size += changesLog.getSize();
      }
      return size;
   }

   /**
    * {@inheritDoc}
    */
   public String getSystemId()
   {
      return systemId;
   }

   /**
    * setSystemId.
    * 
    * @param systemId
    */
   public void setSystemId(String systemId)
   {
      this.systemId = systemId;
   }

   public ItemState getItemState(String itemIdentifier)
   {
      List<ItemState> allStates = getAllStates();
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState state = allStates.get(i);
         if (state.getData().getIdentifier().equals(itemIdentifier))
            return state;
      }
      return null;
   }

   public ItemState getItemState(NodeData parentData, QPathEntry name)
   {
      List<ItemState> allStates = getAllStates();
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState state = allStates.get(i);
         if (state.getData().getParentIdentifier().equals(parentData.getIdentifier())
                  && state.getData().getQPath().getEntries()[state.getData().getQPath().getEntries().length - 1]
                           .isSame(name))
            return state;
      }
      return null;
   }

   public List<ItemState> getChildrenChanges(String rootIdentifier, boolean forNodes)
   {
      List<ItemState> list = new ArrayList<ItemState>();
      for (ItemState state : getAllStates())
      {
         ItemData item = state.getData();
         if (item.getParentIdentifier().equals(rootIdentifier) && item.isNode() == forNodes)
            list.add(state);
      }
      return list;
   }

   /**
    * Find if the node ancestor was renamed in this changes log.
    * 
    * @param item - target node
    * @return - the pair of states of item ancestor, ItemState[] {DELETED,
    *         RENAMED} or null if renaming is not detected.
    * @throws IllegalPathException
    */
   @Deprecated
   public ItemState[] findRenamed(ItemData item) throws IllegalPathException
   {
      List<ItemState> allStates = getAllStates();
      // search from the end for DELETED state.
      // RENAMED comes after the DELETED in the log immediately
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState state = allStates.get(i);
         if (state.getState() == ItemState.DELETED && !state.isPersisted()
                  && item.getQPath().isDescendantOf(state.getData().getQPath()))
         {
            // 1. if it's a parent or the parent is descendant of logged data
            try
            {
               ItemState delete = state;
               ItemState rename = allStates.get(i + 1);

               if (rename.getState() == ItemState.RENAMED && rename.isPersisted()
                        && rename.getData().getIdentifier().equals(delete.getData().getIdentifier()))
               {

                  // 2. search of most fresh state of rename for searched rename state
                  // (i.e. for ancestor
                  // state of the given node)
                  for (int bi = allStates.size() - 1; bi >= i + 2; bi--)
                  {
                     state = allStates.get(bi);
                     if (state.getState() == ItemState.RENAMED && state.isPersisted()
                              && state.getData().getIdentifier().equals(rename.getData().getIdentifier()))
                     {
                        // got much fresh
                        rename = state;
                        delete = allStates.get(i - 1); // try the fresh delete state
                        if (delete.getData().getIdentifier().equals(rename.getData().getIdentifier()))
                           return new ItemState[]
                           {delete, rename}; // 3. ok, got it
                     }
                  }

                  return new ItemState[]
                  {delete, rename}; // 4. ok, there are no
                  // more fresh we have
                  // found before p.2
               } // else, it's not a rename, search deeper
            }
            catch (IndexOutOfBoundsException e)
            {
               // the pair not found
               return null;
            }
         }
      }
      return null;
   }

   public String dump()
   {
      String str = "ChangesLog: size" + changesLogs.size() + "\n ";
      for (PlainChangesLog cLog : changesLogs)
      {
         str += cLog.dump() + "\n";
      }
      return str;
   }

   // Need for Externalizable
   // ------------------ [ BEGIN ] ------------------

   public void writeExternal(ObjectOutput out) throws IOException
   {
      // write -1 if systemId == null
      // write 1 if systemId != null

      if (systemId != null)
      {
         out.writeInt(1);
         out.writeInt(systemId.getBytes().length);
         out.write(systemId.getBytes());
      }
      else
      {
         out.writeInt(-1);
      }

      int listSize = changesLogs.size();
      out.writeInt(listSize);
      for (int i = 0; i < listSize; i++)
         out.writeObject(changesLogs.get(i));
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {

      if (in.readInt() == 1)
      {
         byte[] buf = new byte[in.readInt()];
         in.readFully(buf);
         systemId = new String(buf, Constants.DEFAULT_ENCODING);
      }

      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
         changesLogs.add((PlainChangesLogImpl) in.readObject());
   }

   // ------------------ [ END ] ------------------

}
