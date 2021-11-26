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

package org.exoplatform.services.jcr.ext.backup.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 15.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupMessage.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class BackupMessage
{

   public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

   protected final String message;

   protected final Calendar time;

   private String string;

   BackupMessage(String message)
   {
      this.time = Calendar.getInstance();
      this.message = message;
   }

   public String getMessage()
   {
      return message;
   }

   public boolean isError()
   {
      return false;
   }

   public Calendar getTime()
   {
      return (Calendar)time.clone();
   }

   String formatDate(final Calendar date)
   {
      final SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN);
      return df.format(date.getTime());
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof BackupMessage)
      {
         return this.hashCode() == obj.hashCode();
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return toString().hashCode();
   }

   @Override
   public String toString()
   {
      if (string == null)
         return string = (formatDate(getTime()) + ", " + getMessage());
      else
         return string;
   }

}
