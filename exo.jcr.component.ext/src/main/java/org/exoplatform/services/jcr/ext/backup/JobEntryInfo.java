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
package org.exoplatform.services.jcr.ext.backup;

import java.net.URL;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 *  Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 28, 2007
 */
public class JobEntryInfo
{
   /**
    * The type of job.
    */
   private int type;

   /**
    * The state of job.
    */
   private int state;

   /**
    * The URL to storage.
    */
   private URL url;

   /**
    * The identifier to job.
    */
   private Integer id;

   /**
    * The calendar to job.
    */
   private Calendar calendar;

   /**
    * Getting type of job.
    *
    * @return int
    *           return the type of job
    */
   public int getType()
   {
      return type;
   }

   /**
    * Getting the state of job.
    *
    * @return iot
    *           return the state of job
    */
   public int getState()
   {
      return state;
   }

   /**
    * Getting URL to storage.
    *
    * @return URL
    *           return the url to storage
    */
   public URL getURL()
   {
      return url;
   }

   /**
    * Get date.
    *
    * @return Calendar
    *           return the time stamp
    */
   public Calendar getDate()
   {
      return calendar;
   }

   /**
    * Setting type.
    *
    * @param type
    *          int, the type to job
    */
   public void setType(int type)
   {
      this.type = type;
   }

   /**
    * Setting state.
    *
    * @param state
    *          int, the state to job
    */
   public void setState(int state)
   {
      this.state = state;
   }

   /**
    * Setting storage URL.
    *
    * @param url
    *          URL, the storage url
    */
   public void setURL(URL url)
   {
      this.url = url;
   }

   /**
    * Setting date.
    *
    * @param calendar
    *          Calendar, the time stamp 
    */
   public void setDate(Calendar calendar)
   {
      this.calendar = calendar;
   }

   /**
    * Setting id to job.
    *
    * @param id
    *          Integer,  the id to job
    */
   public void setID(Integer id)
   {
      this.id = id;
   }

   /**
    * Getting id to job.
    *
    * @return Integer
    *           return the id of job
    */
   public Integer getID()
   {
      return id;
   }
}
