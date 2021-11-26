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
 * Read only implementation of {@link ChangedSizeHandler}.
 * Invoking getters methods is committing by invoking appropriate method
 * in delegated {@link ChangedSizeHandler} instance. All setters are forbidden.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: CalculatedContentSizeHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DelegatedContentSizeHandler implements ChangedSizeHandler
{

   /**
    * Delegated {@link ChangedSizeHandler} instance.
    */
   protected ChangedSizeHandler delegated;

   /**
    * DelegatedContentSizeHandler constructor.
    */
   public DelegatedContentSizeHandler(ChangedSizeHandler sizeHandler)
   {
      this.delegated = sizeHandler;
   }

   /**
    * {@inheritDoc}
    */
   public long getChangedSize()
   {
      return delegated.getChangedSize();
   }

   /**
    * {@inheritDoc}
    */
   public long getNewSize()
   {
      return delegated.getNewSize();
   }

   /**
    * {@inheritDoc}
    */
   public long getPrevSize()
   {
      return delegated.getPrevSize();
   }

   /**
    * {@inheritDoc}
    */
   public void accumulateNewSize(long deltaSize)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public void accumulatePrevSize(long deltaSize)
   {
      throw new UnsupportedOperationException("Method is not supported");
   }
}
