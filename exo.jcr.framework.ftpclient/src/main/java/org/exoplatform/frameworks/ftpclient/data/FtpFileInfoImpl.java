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

      StringBuilder _date = new StringBuilder();
      while (fileL.charAt(0) != ' ')
      {
         _date.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      StringBuilder _time = new StringBuilder();

      while (fileL.charAt(0) != ' ')
      {
         _time.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      StringBuilder _size = new StringBuilder();

      if (fileL.indexOf("<DIR>") == 0)
      {
         collection = true;
         while (fileL.charAt(0) != ' ')
         {
            fileL = fileL.substring(1);
         }
         _size = new StringBuilder("0");
      }
      else
      {
         while (fileL.charAt(0) != ' ')
         {
            _size.append(fileL.charAt(0));
            fileL = fileL.substring(1);
         }
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _name = fileL;

      this.name = _name;
      this.date = _date.toString();
      this.time = _time.toString();
      this.size = new Long(_size.toString());

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

      StringBuilder _size = new StringBuilder();
      while (fileL.charAt(0) != ' ')
      {
         _size.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      StringBuilder _month = new StringBuilder();
      while (fileL.charAt(0) != ' ')
      {
         _month.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      StringBuilder _day = new StringBuilder();
      while (fileL.charAt(0) != ' ')
      {
         _day.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      StringBuilder _time = new StringBuilder();
      while (fileL.charAt(0) != ' ')
      {
         _time.append(fileL.charAt(0));
         fileL = fileL.substring(1);
      }

      while (fileL.charAt(0) == ' ')
      {
         fileL = fileL.substring(1);
      }

      String _name = fileL;

      this.name = _name;
      this.date = _month.toString() + " " + _day.toString();
      this.time = _time.toString();
      this.size = new Long(_size.toString());

      return false;
   }

}
