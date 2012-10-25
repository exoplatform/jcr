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

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransactionChangesLog.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TransactionChangesLog implements CompositeChangesLog, Externalizable
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TransactionChangesLog");

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

   /**
    * {@inheritDoc}
    */
   public void removeLog(PlainChangesLog log)
   {
      changesLogs.remove(log);
   }

   /**
    * {@inheritDoc}
    */
   public ChangesLogIterator getLogIterator()
   {
      return new ChangesLogIterator(changesLogs);
   }

   /**
    * {@inheritDoc}
    */
   public List<ItemState> getAllStates()
   {
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

   /**
    * {@inheritDoc}
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
      ItemState state;
      for (int i = changesLogs.size() - 1; i >= 0; i--)
      {
         PlainChangesLog changesLog = changesLogs.get(i);
         state = ((PlainChangesLogImpl)changesLog).getItemState(itemIdentifier);
         if (state != null)
         {
            return state;
         }
      }
      return null;
   }

   public ItemState getItemState(NodeData parentData, QPathEntry name, ItemType itemType)
   {
      ItemState state;
      for (int i = changesLogs.size() - 1; i >= 0; i--)
      {
         PlainChangesLog changesLog = changesLogs.get(i);
         try
         {
            state = ((PlainChangesLogImpl)changesLog).getItemState(parentData, name, itemType);
            if (state != null)
            {
               return state;
            }
         }
         catch (IllegalPathException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
      }
      return null;
   }

   public List<ItemState> getChildrenChanges(String rootIdentifier, boolean forNodes)
   {
      List<ItemState> list = new ArrayList<ItemState>();
      for (PlainChangesLog changesLog : changesLogs)
      {
         List<ItemState> subList = ((PlainChangesLogImpl)changesLog).getChildrenChanges(rootIdentifier, forNodes);
         list.addAll(subList);
      }
      return list;
   }

   public String dump()
   {
      StringBuilder str = new StringBuilder("ChangesLog: size").append(changesLogs.size()).append("\n ");
      for (PlainChangesLog cLog : changesLogs)
      {
         str.append(cLog.dump()).append("\n");
      }
      return str.toString();
   }

   // Need for Externalizable
   // ------------------ [ BEGIN ] ------------------

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // write -1 if systemId == null
      // write 1 if systemId != null

      if (systemId != null)
      {
         out.writeInt(1);

         byte[] buff = systemId.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buff.length);
         out.write(buff);
      }
      else
      {
         out.writeInt(-1);
      }

      int listSize = changesLogs.size();
      out.writeInt(listSize);
      for (int i = 0; i < listSize; i++)
      {
         out.writeObject(changesLogs.get(i));
      }
   }


   /**
    * {@inheritDoc}
    */
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
      {
         changesLogs.add((PlainChangesLog)in.readObject());
      }
   }

   // ------------------ [ END ] ------------------

}
