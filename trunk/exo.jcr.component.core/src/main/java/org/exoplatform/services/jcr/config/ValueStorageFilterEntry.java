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
package org.exoplatform.services.jcr.config;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueStorageFilterEntry.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ValueStorageFilterEntry
{
   private String propertyType;

   private String ancestorPath;

   private String propertyName;

   private long minValueSize;

   public long getMinValueSize()
   {
      return minValueSize;
   }

   public void setMinValueSize(long minValueSize)
   {
      this.minValueSize = minValueSize;
   }

   public ValueStorageFilterEntry()
   {
   }

   public String getAncestorPath()
   {
      return ancestorPath;
   }

   public void setAncestorPath(String ancestorPath)
   {
      this.ancestorPath = ancestorPath;
   }

   public String getPropertyName()
   {
      return propertyName;
   }

   public void setPropertyName(String propertyName)
   {
      this.propertyName = propertyName;
   }

   public String getPropertyType()
   {
      return propertyType;
   }

   public void setPropertyType(String propertyType)
   {
      this.propertyType = propertyType;
   }
}
