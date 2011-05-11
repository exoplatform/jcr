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
