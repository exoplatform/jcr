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
 * @version $Id: CachePatternNodesId.java 111 10.05.2011 serg $
 */
public class CachePatternNodesId extends CacheKey
{

   CachePatternNodesId()
   {
      super();
   }

   CachePatternNodesId(String id)
   {
      super(id);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof CachePatternNodesId)
      {
         CachePatternNodesId cacheNodesId = (CachePatternNodesId)obj;
         return (cacheNodesId.hash == hash && cacheNodesId.fullId.equals(fullId));
      }
      else
      {
         return false;
      }
   }
}
