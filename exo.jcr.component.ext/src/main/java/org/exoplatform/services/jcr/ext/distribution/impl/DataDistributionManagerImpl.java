/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.ext.distribution.impl;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;

/**
 * The default implementation of a {@link DataDistributionManager}.
 * It will use a {@link DataDistributionByName} when the readable mode is expected
 * and a {@link DataDistributionByHash} when the non readable mode is expected
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class DataDistributionManagerImpl implements DataDistributionManager
{

   /**
    * The {@link DataDistributionType} used in case of "readable" mode
    */
   private DataDistributionType readable = new DataDistributionByName();
   
   /**
    * The {@link DataDistributionType} used in case of "optimized" mode
    */
   private DataDistributionType optimized = new DataDistributionByHash();
   
   /**
    * The {@link DataDistributionType} used in case of "none" mode
    */
   private DataDistributionType none = new DataDistributionByPath();
   
   public DataDistributionManagerImpl()
   {
      this(null);
   }
   
   public DataDistributionManagerImpl(InitParams params)
   {
      if (params != null)
      {
         ObjectParameter op = params.getObjectParam("readable");
         if (op != null && op.getObject() instanceof DataDistributionType)
         {
            this.readable = (DataDistributionType)op.getObject();
         }
         op = params.getObjectParam("optimized");
         if (op != null && op.getObject() instanceof DataDistributionType)
         {
            this.optimized = (DataDistributionType)op.getObject();
         }
         op = params.getObjectParam("none");
         if (op != null && op.getObject() instanceof DataDistributionType)
         {
            this.none = (DataDistributionType)op.getObject();
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public DataDistributionType getDataDistributionType(DataDistributionMode mode)
   {
      if (mode == DataDistributionMode.READABLE)
      {
         return readable;
      }
      else if (mode == DataDistributionMode.OPTIMIZED)
      {
         return optimized;
      }
      else if (mode == DataDistributionMode.NONE)
      {
         return none;
      }
      return null;
   }
}
