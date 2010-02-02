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

import org.exoplatform.services.jcr.impl.Constants;

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
 * @version $Id$
 *          Stores collection of ItemStates
 */
public class PlainChangesLogImpl implements Externalizable, PlainChangesLog
{
   private static final int NULL_VALUE = -1;
   
   private static final int NOT_NULL_VALUE = 1;

   private static final long serialVersionUID = 5624550860372364084L;

   protected List<ItemState> items;

   protected String sessionId;

   protected int eventType;

   /**
    * Identifier of system and non-system logs pair. Null if no pair found. 
    */
   protected String pairId = null;

   /**
    * Full qualified constructor.
    * 
    * @param items List of ItemState
    * @param sessionId String 
    * @param eventType int
    * @param pairId String
    */
   public PlainChangesLogImpl(List<ItemState> items, String sessionId, int eventType, String pairId)
   {
      this.items = items;
      this.sessionId = sessionId;
      this.eventType = eventType;
      this.pairId = pairId;
   }

   /**
    * Constructor.
    * 
    * @param items List of ItemState
    * @param sessionId String 
    * @param eventType int
    */
   public PlainChangesLogImpl(List<ItemState> items, String sessionId, int eventType)
   {
      this.items = items;
      this.sessionId = sessionId;
      this.eventType = eventType;
   }

   /**
    * Constructor with undefined event type.
    * 
    * @param items List of ItemState
    * @param sessionId String 
    */
   public PlainChangesLogImpl(List<ItemState> items, String sessionId)
   {
      this(items, sessionId, -1);
   }

   /**
    * An empty log.
    * 
    * @param sessionId String
    */
   public PlainChangesLogImpl(String sessionId)
   {
      this(new ArrayList<ItemState>(), sessionId);
   }

   /**
    * default constructor (for externalizable mainly)
    */
   public PlainChangesLogImpl()
   {
      this(new ArrayList<ItemState>(), null);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemDataChangesLog#getAllStates()
    */
   public List<ItemState> getAllStates()
   {
      return items;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemDataChangesLog#getSize()
    */
   public int getSize()
   {
      return items.size();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.PlainChangesLog#getEventType()
    */
   public int getEventType()
   {
      return eventType;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemDataChangesLog#getSessionId()
    */
   public String getSessionId()
   {
      return sessionId;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.PlainChangesLog#add(org.exoplatform.services.jcr.dataflow
    *      .ItemState)
    */
   public PlainChangesLog add(ItemState change)
   {
      items.add(change);
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.PlainChangesLog#addAll(java.util.List)
    */
   public PlainChangesLog addAll(List<ItemState> changes)
   {
      items.addAll(changes);
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.dataflow.PlainChangesLog#clear()
    */
   public void clear()
   {
      items.clear();
   }

   /**
    * Return pair Id for linked logs (when original log splitted into two, for system and non-system workspaces).
    *
    * @return String
    *           pair identifier 
    */
   public String getPairId()
   {
      return pairId;
   }

   public String dump()
   {
      String str = "ChangesLog: \n";
      for (int i = 0; i < items.size(); i++)
         str +=
            " " + ItemState.nameFromValue(items.get(i).getState()) + "\t" + items.get(i).getData().getIdentifier()
               + "\t" + "isPersisted=" + items.get(i).isPersisted() + "\t" + "isEventFire="
               + items.get(i).isEventFire() + "\t" + "isInternallyCreated=" + items.get(i).isInternallyCreated() + "\t"
               + items.get(i).getData().getQPath().getAsString() + "\n";

      return str;
   }

   // Need for Externalizable
   // ------------------ [ BEGIN ] ------------------

   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(eventType);

      byte[] buff = sessionId.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buff.length);
      out.write(buff);

      int listSize = items.size();
      out.writeInt(listSize);
      for (int i = 0; i < listSize; i++)
      {
         out.writeObject(items.get(i));
      }

      if (pairId != null) 
      {
         out.writeInt(NOT_NULL_VALUE);
         buff = pairId.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buff.length);
         out.write(buff);
      }
      else
      {
         out.writeInt(NULL_VALUE);
      }
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      eventType = in.readInt();

      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      sessionId = new String(buf, Constants.DEFAULT_ENCODING);

      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
      {
         add((ItemState)in.readObject());
      }
      
      if (in.readInt() == NOT_NULL_VALUE)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         pairId = new String(buf, Constants.DEFAULT_ENCODING);
      }
   }
   // ------------------ [ END ] ------------------
}
