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

package org.exoplatform.services.jcr.dataflow;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.observation.ExtendedEventType;

import java.util.List;

/**
 * Created by The eXo Platform SAS<br>
 *
 * Plain changes log implementation (i.e. no nested logs inside)
 * 
 * @author Gennady Azarenkov
 * @LevelAPI Unsupported
 */
public interface PlainChangesLog extends ItemStateChangesLog
{

   /**
    * Return Sesion Id.
    * 
    * @return sessionId of a session produced this changes log
    */
   String getSessionId();

   /**
    * Return pair Id of system and non-system logs.
    * 
    * @return pairId of a pair, null if no pair found.
    */
   String getPairId();

   /**
    * Return this log event type.
    * 
    * @return int, event type produced this log
    * @see ExtendedEventType
    */
   int getEventType();

   /**
    * Adds an item state object to the bottom of this log.
    * 
    * @param state ItemState
    */
   PlainChangesLog add(ItemState state);

   /**
    * Adds list of states object to the bottom of this log.
    *  
    * @param states List of ItemState
    */
   PlainChangesLog addAll(List<ItemState> states);

   /**
    * Returns session instance is present
    * @return session instance
    */
   ExtendedSession getSession();
}
