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

package org.exoplatform.services.jcr.webdav;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class Range
{

   /**
    * Range start value.
    */
   private long start;

   /**
    * Range end value.
    */
   private long end;

   /**
    * Start range getter property.
    * 
    * @return Range start value
    */
   public long getStart()
   {
      return start;
   }

   /**
    * Start range setter property.
    * 
    * @param start Range start value
    */
   public void setStart(long start)
   {
      this.start = start;
   }

   /**
    * End Range getter property.
    * 
    * @return Range end value
    */
   public long getEnd()
   {
      return end;
   }

   /**
    * End Range setter property.
    * 
    * @param end Range end value
    */
   public void setEnd(long end)
   {
      this.end = end;
   }

}
