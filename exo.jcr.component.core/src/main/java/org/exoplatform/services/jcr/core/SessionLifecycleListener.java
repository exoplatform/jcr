/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.core;

/**
 * Created by The eXo Platform SAS<br>
 *
 * A listener allowing to be notified when a session is closed
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SessionLifecycleListener.java 12096 2008-03-19 11:42:40Z gazarenkov $
 * @LevelAPI Experimental
 */

public interface SessionLifecycleListener
{
   /**
    * Called when a session is closed.
    *
    * @param session the session instance
    */
   void onCloseSession(ExtendedSession session);
}
