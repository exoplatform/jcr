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
package org.exoplatform.frameworks.ftpclient.data;

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Vitaly Guly
 * @version $Id: $
 */

public class FtpFileInfoImpl implements FtpFileInfo
{

   private static Log log = ExoLogger.getLogger("exo.jcr.framework.command.FtpFileInfoImpl");

   protected String name = "";

   protected long size = 0;

   protected boolean collection = true;

   protected String date = "";

   protected String time = "";

   public void setName(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name;
   }

   public void setSize(long size)
   {
      this.size = size;
   }

   public long getSize()
   {
      return size;
   }

   public void setType(boolean collection)
   {
      this.collection = collection;
   }

   public boolean isCollection()
   {
      return collection;
   }

   public void setDate(String date)
   {
      this.date = date;
   }

   public String getDate()
   {
      return date;
   }

   public void setTime(String time)
   {
      this.time = time;
   }

   public String getTime()
   {
      return time;
   }

   public boolean parseDir(String fileLine, String systemType)
   {
      if (systemType.startsWith(FtpConst.SysTypes.WINDOWS_NT))
      {
         return parseWindowsNT(fileLine);
      }
      if (systemType.startsWith(FtpConst.SysTypes.UNIX_L8))
      {
         return parseUnixL8(fileLine);
      }
      return false;
   }

   protected boolean parseWindowsNT(String fileLine)
   {
      String fileL = fileLine.substring(0);

      String _date = "";
      while (fileL.charAt(0) != ' ')
      {
         _date += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _time = "";

      while (fileL.charAt(0) != ' ')
      {
         _time += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _size = "";

      if (fileL.indexOf("<DIR>") == 0)
      {
         collection = true;
         while (fileL.charAt(0) != ' ')
         {
            fileL = fileL.substring(1);
         }
         _size = "0";
      }
      else
      {
         while (fileL.charAt(0) != ' ')
         {
            _size += fileL.charAt(0);
            fileL = fileL.substring(1);
         }
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _name = fileL;

      this.name = _name;
      this.date = _date;
      this.time = _time;
      this.size = new Long(_size);

      return false;
   }

   protected boolean parseUnixL8(String fileLine)
   {
      String fileL = fileLine;

      if (fileL.charAt(0) == 'd')
      {
         collection = true;
      }
      else
      {
         collection = false;
      }

      while (fileL.charAt(0) != ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) != ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) != ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) != ' ')
      {
         fileL = fileL.substring(1);
      }
      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _size = "";
      while (fileL.charAt(0) != ' ')
      {
         _size += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _month = "";
      while (fileL.charAt(0) != ' ')
      {
         _month += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _day = "";
      while (fileL.charAt(0) != ' ')
      {
         _day += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _time = "";
      while (fileL.charAt(0) != ' ')
      {
         _time += fileL.charAt(0);
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _name = fileL;

      this.name = _name;
      this.date = _month + " " + _day;
      this.time = _time;
      this.size = new Long(_size);

      return false;
   }

}
