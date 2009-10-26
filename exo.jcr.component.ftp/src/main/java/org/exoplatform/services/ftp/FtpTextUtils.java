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
package org.exoplatform.services.ftp;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class FtpTextUtils
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpTextUtils");

   public static String getStrached(String strVal, int reqLen)
   {
      try
      {
         String datka = "";
         for (int i = 0; i < reqLen; i++)
         {
            if (i >= strVal.length())
            {
               datka += " ";
            }
            else
            {
               datka += strVal.charAt(i);
            }
         }
         return datka;
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage());
         exc.printStackTrace();
      }
      String resStr = "";
      for (int i = 0; i < reqLen; i++)
      {
         resStr += " ";
      }
      return resStr;
   }

   public static String getStrachedAtStart(String strVal, int reqLen)
   {
      String result = strVal;
      while (result.length() < reqLen)
      {
         result = " " + result;
      }
      return result;
   }

}
