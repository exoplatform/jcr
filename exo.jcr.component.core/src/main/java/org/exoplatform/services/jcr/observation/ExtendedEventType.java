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
package org.exoplatform.services.jcr.observation;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.observation.Event;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class ExtendedEventType
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ExtendedEventType");

   public static final String NODE_ADDED = "Node is added";

   public static final String NODE_REMOVED = "Node is removed";

   public static final String PROPERTY_ADDED = "Property is added";

   public static final String PROPERTY_REMOVED = "Property is removed";

   public static final String PROPERTY_CHANGED = "Property is changed";

   public static final String NODE_MOVED = "Node is moved";

   public static final String SAVE = "Save";

   public static final String MOVE = "Move";

   public static final String COPY = "Copy";

   public static final String ADD_MIXIN = "Mixin is added";

   public static final String REMOVE_MIXIN = "Mixin is removed";

   public static final String CLONE = "Clone";

   public static final String UPDATE = "Item is updated";

   public static final String IMPORT = "Import";

   public static final String CHECKIN = "Checkin";

   public static final String CHECKOUT = "Checkout";

   public static final String RESTORE = "Restore";

   public static final String MERGE = "Merge";

   public static final String CANCEL_MERGE = "Cancel merge";

   public static final String DONE_MERGE = "Done merge";

   public static final String ADD_VERSION_LABEL = "Version label added";

   public static final String REMOVE_VERSION_LABEL = "Version label removed";

   public static final String REMOVE_VERSION = "Version removed";

   public static final String LOCK = "Lock";

   public static final String UNLOCK = "Unlock";

   public static final String READ = "Read";

   public static String nameFromValue(int event)
   {
      switch (event)
      {
         case Event.NODE_ADDED :
            return NODE_ADDED;
         case Event.NODE_REMOVED :
            return NODE_REMOVED;
         case Event.PROPERTY_ADDED :
            return PROPERTY_ADDED;
         case Event.PROPERTY_REMOVED :
            return PROPERTY_REMOVED;
         case Event.PROPERTY_CHANGED :
            return PROPERTY_CHANGED;
         case ExtendedEvent.NODE_MOVED :
            return NODE_MOVED;
         case ExtendedEvent.SAVE :
            return SAVE;
         case ExtendedEvent.MOVE :
            return MOVE;
         case ExtendedEvent.COPY :
            return COPY;
         case ExtendedEvent.ADD_MIXIN :
            return ADD_MIXIN;
         case ExtendedEvent.REMOVE_MIXIN :
            return REMOVE_MIXIN;
         case ExtendedEvent.CLONE :
            return CLONE;
         case ExtendedEvent.UPDATE :
            return UPDATE;
         case ExtendedEvent.IMPORT :
            return IMPORT;
         case ExtendedEvent.CHECKIN :
            return CHECKIN;
         case ExtendedEvent.CHECKOUT :
            return CHECKOUT;
         case ExtendedEvent.RESTORE :
            return RESTORE;
         case ExtendedEvent.MERGE :
            return MERGE;
         case ExtendedEvent.CANCEL_MERGE :
            return CANCEL_MERGE;
         case ExtendedEvent.DONE_MERGE :
            return DONE_MERGE;
         case ExtendedEvent.ADD_VERSION_LABEL :
            return ADD_VERSION_LABEL;
         case ExtendedEvent.REMOVE_VERSION_LABEL :
            return REMOVE_VERSION_LABEL;
         case ExtendedEvent.REMOVE_VERSION :
            return REMOVE_VERSION;
         case ExtendedEvent.LOCK :
            return LOCK;
         case ExtendedEvent.UNLOCK :
            return UNLOCK;
         case ExtendedEvent.READ :
            return READ;
         default :
            return "";
      }

   }

   public static int valueFromName(String name)
   {
      if (name.equals(NODE_ADDED))
         return Event.NODE_ADDED;
      else if (name.equals(NODE_REMOVED))
         return Event.NODE_REMOVED;
      else if (name.equals(PROPERTY_ADDED))
         return Event.PROPERTY_ADDED;
      else if (name.equals(PROPERTY_REMOVED))
         return Event.PROPERTY_REMOVED;
      else if (name.equals(PROPERTY_CHANGED))
         return Event.PROPERTY_CHANGED;
      else if (name.equals(NODE_MOVED))
         return ExtendedEvent.NODE_MOVED;
      else if (name.equals(SAVE))
         return ExtendedEvent.SAVE;
      else if (name.equals(MOVE))
         return ExtendedEvent.MOVE;
      else if (name.equals(COPY))
         return ExtendedEvent.COPY;
      else if (name.equals(ADD_MIXIN))
         return ExtendedEvent.ADD_MIXIN;
      else if (name.equals(REMOVE_MIXIN))
         return ExtendedEvent.REMOVE_MIXIN;
      else if (name.equals(CLONE))
         return ExtendedEvent.CLONE;
      else if (name.equals(UPDATE))
         return ExtendedEvent.UPDATE;
      else if (name.equals(IMPORT))
         return ExtendedEvent.IMPORT;
      else if (name.equals(CHECKIN))
         return ExtendedEvent.CHECKIN;
      else if (name.equals(CHECKOUT))
         return ExtendedEvent.CHECKOUT;
      else if (name.equals(RESTORE))
         return ExtendedEvent.RESTORE;
      else if (name.equals(MERGE))
         return ExtendedEvent.MERGE;
      else if (name.equals(CANCEL_MERGE))
         return ExtendedEvent.CANCEL_MERGE;
      else if (name.equals(DONE_MERGE))
         return ExtendedEvent.DONE_MERGE;
      else if (name.equals(ADD_VERSION_LABEL))
         return ExtendedEvent.ADD_VERSION_LABEL;
      else if (name.equals(REMOVE_VERSION_LABEL))
         return ExtendedEvent.REMOVE_VERSION_LABEL;
      else if (name.equals(REMOVE_VERSION))
         return ExtendedEvent.REMOVE_VERSION;
      else if (name.equals(LOCK))
         return ExtendedEvent.LOCK;
      else if (name.equals(UNLOCK))
         return ExtendedEvent.UNLOCK;
      else if (name.equals(READ))
         return ExtendedEvent.READ;
      else
         return -1;
   }
}
