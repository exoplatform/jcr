/*
 * Copyright (C) 2011 eXo Platform SAS.
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
