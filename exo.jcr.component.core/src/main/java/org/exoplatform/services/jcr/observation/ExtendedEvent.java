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

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ExtendedEvent.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface ExtendedEvent extends Event
{

   public static final int SAVE = 32;

   public static final int MOVE = 64;

   public static final int COPY = 128;

   public static final int ADD_MIXIN = 256;

   public static final int REMOVE_MIXIN = 512;

   public static final int CLONE = 1024;

   public static final int UPDATE = 2048;

   public static final int IMPORT = 4096;

   public static final int CHECKIN = 8192;

   public static final int CHECKOUT = 16384;

   public static final int RESTORE = 32768;

   public static final int MERGE = 65536;

   public static final int CANCEL_MERGE = 131072;

   public static final int DONE_MERGE = 262144;

   public static final int ADD_VERSION_LABEL = 524288;

   public static final int REMOVE_VERSION_LABEL = 1048576;

   public static final int REMOVE_VERSION = 2097152;

   public static final int LOCK = 4194304;

   public static final int UNLOCK = 8388608;

   public static final int READ = 16777216;

   public static final int NODE_MOVED  = 33554432;

   /**
    * Returns the identifier associated with this event or <code>null</code> if
    * this event has no associated identifier. The meaning of the associated
    * identifier depends upon the type of the event. See event type constants
    * above.
    *
    * @return the identifier associated with this event or <code>null</code>.
    * @throws javax.jcr.RepositoryException if an error occurs.
    * @since JCR 2.0
    */
   public String getIdentifier() throws RepositoryException;

   /**
    * Returns the information map associated with this event. The meaning of
    * the map depends upon the type of the event. See event type constants
    * above.
    *
    * @return A <code>Map</code> containing parameter information for instances
    *         of a <code>NODE_MOVED</code> event.
    * @throws RepositoryException if an error occurs.
    * @since JCR 2.0
    */
   public Map getInfo() throws RepositoryException;

   /**
    * Returns the user data set through {@link ObservationManager#setUserData}
    * on the <code>ObservationManager</code> bound to the <code>Session</code>
    * that caused the event.
    *
    * @return The user data string.
    * @throws RepositoryException if an error occurs.
    * @since JCR 2.0
    */
   public String getUserData() throws RepositoryException;

   /**
    * Returns the date when the change was persisted that caused this event.
    * The date is represented as a millisecond value that is an offset from the
    * Epoch, January 1, 1970 00:00:00.000 GMT (Gregorian). The granularity of
    * the returned value is implementation dependent.
    *
    * @return the date when the change was persisted that caused this event.
    * @throws RepositoryException if an error occurs.
    * @since JCR 2.0
    */
   public long getDate() throws RepositoryException;

}
