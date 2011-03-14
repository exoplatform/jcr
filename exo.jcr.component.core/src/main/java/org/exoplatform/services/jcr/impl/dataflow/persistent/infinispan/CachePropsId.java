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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 10.06.2008
 * 
 * Cache record used to store item Id key.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CachePropsId.java 2845 2010-07-30 13:29:37Z tolusha $
 */
public class CachePropsId extends CacheKey
{

   CachePropsId(String id)
   {
      super(id);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof CachePropsId)
      {
         CachePropsId cachePropsId = (CachePropsId)obj;
         return (cachePropsId.hash == hash && cachePropsId.fullId.equals(fullId));
      }
      else
      {
         return false;
      }
   }
}
