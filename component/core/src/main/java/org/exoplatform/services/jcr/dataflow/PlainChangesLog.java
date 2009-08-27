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

import java.util.List;

import org.exoplatform.services.jcr.observation.ExtendedEventType;

/**
 * Created by The eXo Platform SAS.<br/> Plain changes log implementation (i.e. no nested logs
 * inside)
 * 
 * @author Gennady Azarenkov
 * @version $Id: PlainChangesLog.java 11907 2008-03-13 15:36:21Z ksm $
 */
public interface PlainChangesLog
   extends ItemStateChangesLog
{

   /**
    * @return sessionId of a session produced this changes log
    */
   String getSessionId();

   /**
    * @return event type produced this log
    * @see ExtendedEventType
    */
   int getEventType();

   /**
    * adds an item state object to the bottom of this log
    * 
    * @param state
    */
   PlainChangesLog add(ItemState state);

   /**
    * adds list of states object to the bottom of this log
    * 
    * @param states
    */
   PlainChangesLog addAll(List<ItemState> states);
}
