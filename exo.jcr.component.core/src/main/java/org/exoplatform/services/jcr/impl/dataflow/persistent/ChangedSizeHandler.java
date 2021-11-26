/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.dataflow.persistent;

/**
 * Accumulates data size changes at persisted level. It is used to tracks
 * node data size changes.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ContentSizeHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public interface ChangedSizeHandler
{

   /**
    * Returns difference between new and previous content sizes.
    */
   long getChangedSize();

   /**
    * Returns new content size.
    */
   long getNewSize();

   /**
    * Returns previous content size.
    */
   long getPrevSize();

   /**
    *  Accumulates size of content to be written into storage.
    */
   void accumulateNewSize(long deltaSize);

   /**
    * Accumulates size of content already existed into storage to be replaced.
    */
   void accumulatePrevSize(long deltaSize);

}