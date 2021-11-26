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
 * Simple implementation of {@link ChangedSizeHandler}. Merely accumulates
 * and returns values.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: SimpleChangedSizeHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SimpleChangedSizeHandler implements ChangedSizeHandler
{
   /**
    * Total size of newly wrote content size.
    */
   protected long newSize;

   /**
    * Total size of content stored into storage before.
    */
   protected long prevSize;

   /**
    * {@inheritDoc}
    */
   public long getChangedSize()
   {
      return newSize - prevSize;
   }

   /**
    * {@inheritDoc}
    */
   public long getNewSize()
   {
      return newSize;
   }

   /**
    * {@inheritDoc}
    */
   public void accumulateNewSize(long deltaSize)
   {
      this.newSize += deltaSize;
   }

   /**
    * {@inheritDoc}
    */
   public void accumulatePrevSize(long deltaSize)
   {
      this.prevSize += deltaSize;
   }

   /**
    * {@inheritDoc}
    */
   public long getPrevSize()
   {
      return prevSize;
   }
}
