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
package org.exoplatform.services.jcr.impl.dataflow;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public abstract class AbstractPersistedValueData implements ValueData
{

   protected final static Log LOG = ExoLogger.getLogger("jcr.PersistedValueData");

   protected int orderNumber;

   /**
    *  The empty constructor to serialization.
    */
   protected AbstractPersistedValueData()
   {
   }
   
   protected AbstractPersistedValueData(int orderNumber)
   {
      this.orderNumber = orderNumber;
   }
   
   /**
    * {@inheritDoc}
    */
   public final int getOrderNumber()
   {
      return orderNumber;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof ValueData)
      {
         return this.equals((ValueData)obj);
      }

      return false;
   }

   /**
    * Create transient copy of persisted data.
    * 
    * @return TransientValueData
    * @throws RepositoryException if error ocurs
    */
   public abstract TransientValueData createTransientCopy() throws RepositoryException;
   
}
