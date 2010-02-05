/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.cluster.load;

public class WorkerResult
{

   private final boolean isRead;

   private final long responceTime;

   /**
    * @param isRead
    * @param responceTime
    * @param abstractAvgResponceTimeTest TODO
    */
   public WorkerResult(boolean isRead, long responceTime)
   {
      super();
      this.isRead = isRead;
      this.responceTime = responceTime;
   }

   /**
    * @return the isRead
    */
   public boolean isRead()
   {
      return isRead;
   }

   /**
    * @return the responceTime
    */
   public long getResponceTime()
   {
      return responceTime;
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      return "WorkerResult [isRead=" + isRead + ", responceTime=" + responceTime + "]";
   }

}