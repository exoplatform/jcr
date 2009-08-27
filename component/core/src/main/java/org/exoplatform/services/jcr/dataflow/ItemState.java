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

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.<br/> item state to save
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemState.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ItemState
   implements Externalizable
{

   private static final long serialVersionUID = 7967457831325761318L;

   private static Log log = ExoLogger.getLogger("jcr.ItemState");

   public static final int ADDED = 1;

   public static final int UPDATED = 2;

   public static final int DELETED = 4;

   public static final int UNCHANGED = 8;

   public static final int MIXIN_CHANGED = 16;

   public static final int RENAMED = 32;

   /**
    * underlying item data
    */
   protected ItemData data;

   protected int state;

   private boolean isPersisted = true;

   /**
    * Indicates that item is created internaly by system
    */
   private transient boolean internallyCreated = false;

   /**
    * if storing of this state ahould cause event firing
    */
   protected transient boolean eventFire;

   /**
    * path to the data on which save should be called for this state (for Session.move() for ex)
    */
   private transient QPath ancestorToSave;

   /**
    * The constructor
    * 
    * @param data
    *          underlying data
    * @param state
    * @param eventFire
    *          - if the state cause some event firing
    * @param ancestorToSave
    *          - path of item which should be called in save (usually for session.move())
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave)
   {
      this(data, state, eventFire, ancestorToSave, false, true);
   }

   /**
    * @param data
    *          underlying data
    * @param state
    * @param eventFire
    *          - if the state cause some event firing
    * @param ancestorToSave
    *          - path of item which should be called in save (usually for session.move())
    * @param isInternalCreated
    *          - indicates that item is created internaly by system
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated)
   {
      this(data, state, eventFire, ancestorToSave, isInternalCreated, true);
   }

   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated,
            boolean isPersisted)
   {
      this.data = data;
      this.state = state;
      this.eventFire = eventFire;
      if (ancestorToSave == null)
         this.ancestorToSave = data.getQPath();
      else
         this.ancestorToSave = ancestorToSave;
      this.internallyCreated = isInternalCreated;
      this.isPersisted = isPersisted;

      if (log.isDebugEnabled())
         log.debug(nameFromValue(state) + " " + data.getQPath().getAsString() + ",  " + data.getIdentifier());

   }

   public boolean isPersisted()
   {
      return isPersisted;
   }

   /**
    * @return data.
    */
   public ItemData getData()
   {
      return data;
   }

   /**
    * @return state.
    */
   public int getState()
   {
      return state;
   }

   public boolean isNode()
   {
      return data.isNode();
   }

   public boolean isAdded()
   {
      return state == ADDED;
   }

   public boolean isUpdated()
   {
      return (state == UPDATED);
   }

   public boolean isDeleted()
   {
      return state == DELETED;
   }

   public boolean isUnchanged()
   {
      return (state == UNCHANGED);
   }

   public boolean isMixinChanged()
   {
      return (state == MIXIN_CHANGED);
   }

   public boolean isRenamed()
   {
      return (state == RENAMED);
   }

   public boolean isEventFire()
   {
      return eventFire;
   }

   public void eraseEventFire()
   {
      eventFire = false;
   }

   public void makeLogical()
   {
      isPersisted = false;
   }

   public boolean isDescendantOf(QPath relPath)
   {
      return ancestorToSave.isDescendantOf(relPath) || ancestorToSave.equals(relPath);
   }

   public QPath getAncestorToSave()
   {
      return ancestorToSave;
   }

   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;

      if (obj instanceof ItemState)
      {
         ItemState other = (ItemState) obj;
         return other.getData().equals(data) && other.getState() == state;
      }

      return false;
   }

   public boolean isSame(ItemState item)
   {
      if (this == item)
         return true;

      return this.getData().getIdentifier().hashCode() == item.getData().getIdentifier().hashCode()
               && this.getData().getQPath().hashCode() == item.getData().getQPath().hashCode()
               && this.getState() == item.getState();
   }

   /**
    * Is two item states are same. Added for merger.
    * 
    * isSame.
    * 
    * @param src
    * @param dst
    * @return
    */
   public boolean isSame(String dstIdentifier, QPath dstPath, int dstState)
   {
      return this.getData().getIdentifier().hashCode() == dstIdentifier.hashCode()
               && this.getData().getQPath().hashCode() == dstPath.hashCode() && this.getState() == dstState;
   }

   /**
    * creates ADDED item state shortcut for new ItemState(data, ADDED, true, true, null)
    * 
    * @param data
    * @param needValidation
    * @return
    */
   public static ItemState createAddedState(ItemData data)
   {
      return new ItemState(data, ADDED, true, null, true);
   }

   public static ItemState createAddedState(ItemData data, boolean isInternalCreated)
   {
      return new ItemState(data, ADDED, true, null, isInternalCreated);
   }

   /**
    * creates UPDATED item state shortcut for new ItemState(data, UPDATED, true, true, null)
    * 
    * @param data
    * @param needValidation
    * @return
    */
   public static ItemState createUpdatedState(ItemData data)
   {
      return new ItemState(data, UPDATED, true, null);
   }

   public static ItemState createUpdatedState(ItemData data, boolean isInternalCreated)
   {
      return new ItemState(data, UPDATED, true, null, isInternalCreated);
   }

   /**
    * creates RENAMED item state shortcut for new ItemState(data, RENAMED, true, true, null)
    * 
    * @param data
    * @param needValidation
    * @return
    */
   public static ItemState createRenamedState(ItemData data)
   {
      return new ItemState(data, RENAMED, true, null);
   }

   public static ItemState createRenamedState(ItemData data, boolean isInternalCreated)
   {
      return new ItemState(data, RENAMED, true, null, isInternalCreated);
   }

   /**
    * creates DELETED item state shortcut for new ItemState(data, DELETED, true, true, null)
    * 
    * @param data
    * @param needValidation
    * @return
    */
   public static ItemState createDeletedState(ItemData data)
   {
      return new ItemState(data, DELETED, true, null);
   }

   public static ItemState createDeletedState(ItemData data, boolean isInternalCreated)
   {
      return new ItemState(data, DELETED, true, null, isInternalCreated);
   }

   /**
    * creates UNCHANGED item state shortcut for new ItemState(data, UNCHANGED, false, false, null)
    * 
    * @param data
    * @param needValidation
    * @return
    */
   public static ItemState createUnchangedState(ItemData data)
   {
      return new ItemState(data, UNCHANGED, false, null);
   }

   public static ItemState createUnchangedState(ItemData data, boolean isInternalCreated)
   {
      return new ItemState(data, UNCHANGED, false, null, isInternalCreated);
   }

   public static String nameFromValue(int stateValue)
   {
      switch (stateValue)
      {
         case ADDED :
            return "ADDED";
         case DELETED :
            return "DELETED";
         case UPDATED :
            return "UPDATED";
         case UNCHANGED :
            return "UNCHANGED";
         case MIXIN_CHANGED :
            return "MIXIN_CHANGED";
         case RENAMED :
            return "RENAMED";
         default :
            return "UNDEFINED STATE";
      }
   }

   public boolean isInternallyCreated()
   {
      return internallyCreated;
   }

   // Externalizable --------------------
   public ItemState()
   {
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(state);
      out.writeBoolean(isPersisted);
      out.writeBoolean(eventFire);
      out.writeObject(data);
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      state = in.readInt();
      isPersisted = in.readBoolean();
      eventFire = in.readBoolean();
      data = (ItemData) in.readObject();
   }

}
