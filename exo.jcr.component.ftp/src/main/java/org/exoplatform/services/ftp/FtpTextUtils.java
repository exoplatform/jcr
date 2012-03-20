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

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ftp.FtpTextUtils");

   public static String getStrached(String strVal, int reqLen)
   {
      try
      {
         StringBuilder datka = new StringBuilder();
         for (int i = 0; i < reqLen; i++)
         {
            if (i >= strVal.length())
            {
               datka.append(" ");
            }
            else
            {
               datka.append(strVal.charAt(i));
            }
         }
         return datka.toString();
      }
      catch (Exception exc)
      {
         LOG.info("Unhandled exception. " + exc.getMessage(), exc);
      }
      StringBuilder resStr = new StringBuilder(); 
      for (int i = 0; i < reqLen; i++)
      {
         resStr.append(" ");
      }
      return resStr.toString();
   }

   public static String getStrachedAtStart(String strVal, int reqLen)
   {
      String result = strVal;
      while (result.length() < reqLen)
      {
         result = " " + result; //NOSONAR
      }
      return result;
   }

   public static String replaceForbiddenChars(String strVal, String forbiddenChars, char replaceChar)
   {
      char[] result = new char[strVal.length()];

      for (int i = 0; i < strVal.length(); i++)
      {
         boolean replaced = false;

         for (int j = 0; j < forbiddenChars.length(); j++)
         {
            if (strVal.charAt(i) == forbiddenChars.charAt(j))
            {
               result[i] = replaceChar;
               replaced = true;
               break;
            }
         }

         if (!replaced)
         {
            result[i] = strVal.charAt(i);
         }
      }

      return new String(result);
   }

}
