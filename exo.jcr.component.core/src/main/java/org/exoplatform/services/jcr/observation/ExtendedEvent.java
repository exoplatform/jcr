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

   /**
    * Generated on persist when a node is moved. <ul> <li>{@link #getPath}
    * returns the absolute path of the destination of the move.</li> <li>{@link
    * #getIdentifier} returns the identifier of the moved node. <li> {@link
    * #getInfo} If the method that caused this event was a {@link
    * javax.jcr.Session#move Session.move} or {@link javax.jcr.Workspace#move
    * Workspace.move} then the returned {@link java.util.Map Map} has keys
    * <code>srcAbsPath</code> and <code>destAbsPath</code> with values
    * corresponding to the parameters passed to the <code>move</code> method.
    * <p>
    * If the method that caused this event was a {@link
    * javax.jcr.Node#orderBefore Node.orderBefore} then the returned
    * <code>Map</code> has keys <code>srcChildRelPath</code> and
    * <code>destChildRelPath</code> with values corresponding to the parameters
    * passed to the <code>orderBefore</code> method. </li> </ul>
    */
   public static final int NODE_MOVED  = 33554432;

   /**
    * The key <code>srcAbsPath</code> in the info map.
    */
   public static final String SRC_ABS_PATH = "srcAbsPath";

   /**
    * The key <code>destAbsPath</code> in the info map.
    */
   public static final String DEST_ABS_PATH = "destAbsPath";

   /**
    * The key <code>srcChildRelPath</code> in the info map.
    */
   public static final String SRC_CHILD_REL_PATH = "srcChildRelPath";

   /**
    * The key <code>destChildRelPath</code> in the info map.
    */
   public static final String DEST_CHILD_REL_PATH = "destChildRelPath";

   /**
    * Returns the information map associated with this event. The meaning of
    * the map depends upon the type of the event. See event type constants
    * above.
    *
    * @return A <code>Map</code> containing parameter information for instances
    *         of a <code>NODE_MOVED</code> event.
    * @throws RepositoryException if an error occurs.
    */
   public Map getInfo() throws RepositoryException;
}
