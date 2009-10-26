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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;

/**
 * 
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id:AbstractValueData.java 12534 2007-02-02 15:30:52Z peterit $
 */

public abstract class AbstractValueData implements ValueData
{

   protected final static Log log = ExoLogger.getLogger("jcr.AbstractValueData");

   protected int orderNumber;

   protected AbstractValueData(int orderNumber)
   {
      this.orderNumber = orderNumber;
   }

   public final int getOrderNumber()
   {
      return orderNumber;
   }

   public final void setOrderNumber(int orderNumber)
   {
      this.orderNumber = orderNumber;
   }

   public abstract TransientValueData createTransientCopy() throws RepositoryException;
}
