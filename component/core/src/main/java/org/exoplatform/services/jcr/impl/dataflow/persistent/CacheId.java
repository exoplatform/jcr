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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.QPath;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 10.06.2008
 * 
 * Cache record used to store item Id key.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheId.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class CacheId
   extends CacheKey
{

   /**
    * Item identifier. This id (String) may be placed in childs caches CN, CP (WeakHashMap) as a key.
    * <br/>So, this instance will prevent GC remove them from CN, CP.
    */
   private final String id;

   CacheId(String id)
   {
      this.id = id;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (id.hashCode() == obj.hashCode() && obj instanceof CacheId)
         return id.equals(((CacheId) obj).id);
      return false;
   }

   @Override
   public int hashCode()
   {
      return this.id.hashCode();
   }

   @Override
   public String toString()
   {
      return this.id.toString();
   }

   @Override
   boolean isDescendantOf(QPath path)
   {
      return false;
   }
}
