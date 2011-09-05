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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CachePatternPropsId.java 111 28 квіт. 2011 serg $
 */
public class CachePatternPropsId extends CacheKey
{

   public CachePatternPropsId()
   {
      super();
   }

   CachePatternPropsId(String parentId)
   {
      super(parentId);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof CachePatternPropsId)
      {
         CachePatternPropsId cachePatternPropsId = (CachePatternPropsId)obj;
         return (cachePatternPropsId.hash == hash && cachePatternPropsId.fullId.equals(fullId));
      }
      else
      {
         return false;
      }
   }
}
