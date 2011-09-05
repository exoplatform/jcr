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
package org.exoplatform.services.jcr.storage.value;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValuePluginFilter.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public final class ValuePluginFilter
{

   private final int propertyType;

   private final QPath ancestorPath;

   private final InternalQName propertyName;

   private final long minValueSize;

   /**
    * Default filter (all BINARY properties accepted)
    * 
    * @throws RepositoryConfigurationException
    */
   public ValuePluginFilter() throws RepositoryConfigurationException
   {
      this(PropertyType.BINARY, null, null, 0);
   }

   /**
    * Full qualified filter
    * 
    * @param propertyType
    * @param ancestorPath
    * @param propertyName
    * @throws RepositoryConfigurationException
    */
   public ValuePluginFilter(int propertyType, QPath ancestorPath, InternalQName propertyName, long minValueSize)
      throws RepositoryConfigurationException
   {
      if (propertyType == PropertyType.UNDEFINED)
      {
         throw new RepositoryConfigurationException("Property type is obligatory");
      }
      
      this.propertyType = propertyType;
      this.ancestorPath = ancestorPath;
      this.propertyName = propertyName;
      this.minValueSize = minValueSize > 0 ? minValueSize : -1;
   }

   public QPath getAncestorPath()
   {
      return ancestorPath;
   }

   public InternalQName getPropertyName()
   {
      return propertyName;
   }

   public int getPropertyType()
   {
      return propertyType;
   }

   /**
    * @param prop
    *          - incoming PropertyData
    * @return true if this filter criterias match incoming PropertyData
    */
   public boolean match(PropertyData prop, int valueOrderNumer)
   {
      if (propertyType == prop.getType()
         && (ancestorPath == null || prop.getQPath().isDescendantOf(ancestorPath))
         && (minValueSize == -1 || (prop.getValues().get(valueOrderNumer).getLength() > minValueSize && minValueSize > 0))
         && (propertyName == null || prop.getQPath().getName().equals(propertyName)))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   /**
    * @return minimal value size in bytes or null if not configured
    */
   public long getMinValueSize()
   {
      return minValueSize;
   }

}
