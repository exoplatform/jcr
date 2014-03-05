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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ReadOnlyChangedSizeHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.<br/> item state to save
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemState.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ItemState implements Externalizable
{

   private static final long serialVersionUID = 7967457831325761318L;

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemState");

   public static final int ADDED = 1;

   public static final int UPDATED = 2;

   public static final int DELETED = 4;

   public static final int UNCHANGED = 8;

   public static final int MIXIN_CHANGED = 16;

   public static final int RENAMED = 32;

   public static final int PATH_CHANGED = 64;

   public static final int ADDED_AUTO_CREATED_NODES = 128;

   public static final int MOVED = 256;

   public static final int ORDERED = 512;

   /**
    * underlying item data
    */
   protected ItemData data;

   protected int state;

   private boolean isPersisted = true;

   /**
    * Indicates that item is created internally by system
    */
   private transient boolean internallyCreated = false;

   /**
    * if storing of this state should cause event firing
    */
   protected transient boolean eventFire;

   /**
    * path to the data on which save should be called for this state (for Session.move() for ex)
    */
   private transient QPath ancestorToSave;

   /** 
    * Storing old node path during Session.move() operation 
    */
   private QPath oldPath;

   /**
    * Handler to manage changing in persisted content size.
    */
   private transient ChangedSizeHandler sizeHandler;

   /**
    * ItemState constructor.
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave)
   {
      this(data, state, eventFire, ancestorToSave, false, true);
   }

   /**
    * ItemState constructor.
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated)
   {
      this(data, state, eventFire, ancestorToSave, isInternalCreated, true);
   }

   /**
    * ItemState constructor.
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated,
      boolean isPersisted, QPath oldPath)
   {
      this(data, state, eventFire, ancestorToSave, isInternalCreated, isPersisted);
      this.oldPath = oldPath;
   }

   /**
    * ItemState constructor.
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated,
      boolean isPersisted, QPath oldPath, ChangedSizeHandler sizeHandler)
   {
      this(data, state, eventFire, ancestorToSave, isInternalCreated, isPersisted, oldPath);
      this.sizeHandler = sizeHandler;


   }

   /**
    * ItemState constructor.
    */
   public ItemState(ItemData data, int state, boolean eventFire, QPath ancestorToSave, boolean isInternalCreated,
      boolean isPersisted)
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.INVOKE_INTERNAL_API_PERMISSION);

      this.data = data;
      this.state = state;
      this.eventFire = eventFire;
      if (ancestorToSave == null)
      {
         this.ancestorToSave = data.getQPath();
      }
      else
      {
         this.ancestorToSave = ancestorToSave;
      }
      this.internallyCreated = isInternalCreated;
      this.isPersisted = isPersisted;

      if (LOG.isDebugEnabled())
      {
         LOG.debug(nameFromValue(state) + " " + data.getQPath().getAsString() + ",  " + data.getIdentifier());
      }
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

   public boolean isPathChanged()
   {
      return (state == PATH_CHANGED);
   }

   public boolean isAddedAutoCreatedNodes()
   {
      return (state == ADDED_AUTO_CREATED_NODES);
   }

   public boolean isMoved()
   {
      return state == MOVED;
   }

   public boolean isOrdered()
   {
      return state == ORDERED;
   }

   public boolean isEventFire()
   {
      return eventFire;
   }

   public void eraseEventFire()
   {
      eventFire = false;
   }

   public void makeStateAdded()
   {
      state = ADDED;
   }

   public boolean isDescendantOf(QPath relPath)
   {
      return ancestorToSave.isDescendantOf(relPath) || ancestorToSave.equals(relPath);
   }

   public QPath getAncestorToSave()
   {
      return ancestorToSave;
   }

   public QPath getOldPath()
   {
      return oldPath;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;

      if (obj instanceof ItemState)
      {
         ItemState other = (ItemState)obj;
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

   public static ItemState createAddedAutoCreatedNodes(ItemData data)
   {
      return new ItemState(data, ADDED_AUTO_CREATED_NODES, false, null, false);
   }

   /**
    * creates ADDED item state shortcut for new ItemState(data, ADDED, true, true, null)
    * 
    * @param data
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
    * creates DELETED item state shortcut for new ItemState(data, DELETED, true, true, null)
    * 
    * @param data
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
    * creates RENAMED item state shortcut for new ItemState(data, RENAMED, true, true, null)
    * 
    * @param data
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
    * creates UNCHANGED item state shortcut for new ItemState(data, UNCHANGED, false, false, null)
    * 
    * @param data
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

   public static ItemState createMovedState(ItemData data, QPath oldPath)
   {
      return new ItemState(data, MOVED, true, null,true, false, oldPath);
   }

   public static ItemState createOrderedState(ItemData data, QPath oldPath)
   {
      return new ItemState(data, ORDERED, true, null,true, false, oldPath);
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
         case PATH_CHANGED :
            return "PATH_CHANGED";
         case ADDED_AUTO_CREATED_NODES :
            return "ADDED_AUTO_CREATED_NODES";
         case MOVED :
            return "MOVED";
         case ORDERED :
            return "ORDERED";
         default :
            return "UNDEFINED STATE";
      }
   }

   public boolean isInternallyCreated()
   {
      return internallyCreated;
   }

   /**
    * @see {@link ChangedSizeHandler#getChangedSize()}
    */
   public long getChangedSize()
   {
      if (sizeHandler == null)
      {
         throw new IllegalStateException("Changed size value is undefine. May be it is not appropriate place to invoke");
      }

      return sizeHandler.getChangedSize();
   }

   /**
    * Returns {@link ChangedSizeHandler} instance currently
    * injected.
    */
   public ChangedSizeHandler getChangedSizeHandler()
   {
      if (sizeHandler == null || sizeHandler instanceof ReadOnlyChangedSizeHandler)
      {
         return sizeHandler;
      }

      return new ReadOnlyChangedSizeHandler(sizeHandler.getNewSize(), sizeHandler.getPrevSize());
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

      if (oldPath == null)
      {
         out.writeInt(SerializationConstants.NULL_DATA);
      }
      else
      {
         out.writeInt(SerializationConstants.NOT_NULL_DATA);
         byte[] buf = oldPath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }

      out.writeObject(data);
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      state = in.readInt();
      isPersisted = in.readBoolean();
      eventFire = in.readBoolean();
      
      if (in.readInt() == SerializationConstants.NOT_NULL_DATA)
      {
         byte[] buf = new byte[in.readInt()];
         in.readFully(buf);

         try
         {
            oldPath = QPath.parse(new String(buf, Constants.DEFAULT_ENCODING));
         }
         catch (IllegalPathException e)
         {
            throw new IOException("Data currupted.", e);
         }
      }

      data = (ItemData)in.readObject();
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      return "ItemState [data=" + data + ", state=" + state + ", isPersisted=" + isPersisted + ", internallyCreated="
         + internallyCreated + ", eventFire=" + eventFire + ", ancestorToSave=" + ancestorToSave + ", oldPath="
         + oldPath + "]";
   }
}
