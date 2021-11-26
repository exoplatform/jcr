/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.dataflow.persistent;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 24.06.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheStatistic.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class CacheStatistic
{

   protected final long miss;

   protected final long hits;

   protected final long size;

   protected final long nodesSize;

   protected final long propertiesSize;

   protected final long totalGetTime;

   // configuration

   protected final long maxSize;

   protected final long liveTime;

   CacheStatistic(long miss, long hits, long size, long nodesSize, long propertiesSize, long maxSize, long liveTime,
      long totalGetTime)
   {
      this.maxSize = maxSize;
      this.liveTime = liveTime;
      this.totalGetTime = totalGetTime;

      this.miss = miss;
      this.hits = hits;
      this.size = size;
      this.nodesSize = nodesSize;
      this.propertiesSize = propertiesSize;
   }

   public long getMiss()
   {
      return miss;
   }

   public long getHits()
   {
      return hits;
   }

   public long getSize()
   {
      return size;
   }

   public long getNodesSize()
   {
      return nodesSize;
   }

   public long getPropertiesSize()
   {
      return propertiesSize;
   }

   public long getMaxSize()
   {
      return maxSize;
   }

   public long getLiveTime()
   {
      return liveTime;
   }

   public long getTotalGetTime()
   {
      return totalGetTime;
   }

}
