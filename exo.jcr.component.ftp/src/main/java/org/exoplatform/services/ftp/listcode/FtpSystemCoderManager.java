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
package org.exoplatform.services.ftp.listcode;

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.config.FtpConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpSystemCoderManager
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpSystemCoderManager");

   private static final String[][] availableSysemCoders =
      {{FtpConst.Encoding.WINDOWS_NT, FtpWindowsNTCoder.class.getCanonicalName()},
         {FtpConst.Encoding.UNIX_L8, FtpUnixL8Coder.class.getCanonicalName()}};

   public static FtpSystemCoder getSystemCoder(FtpConfig configuration)
   {
      String systemType = configuration.getSystemType();
      for (int i = 0; i < availableSysemCoders.length; i++)
      {
         if (systemType.equals(availableSysemCoders[i][0]))
         {
            try
            {
               FtpSystemCoder coder =
                  (FtpSystemCoder)ClassLoading.forName(availableSysemCoders[i][1], FtpSystemCoderManager.class).newInstance();
               return coder;
            }
            catch (ClassNotFoundException exc)
            {
               log.info("Unhandled exception. " + exc.getMessage(), exc);
            }
            catch (InstantiationException exc)
            {
               log.info("Unhandled exception. " + exc.getMessage(), exc);
            }
            catch (IllegalAccessException exc)
            {
               log.info("Unhandled exception. " + exc.getMessage(), exc);
            }
         }
      }
      return null;
   }

}
