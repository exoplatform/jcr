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

package org.exoplatform.services.ftp.client;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpClientTimeOutThread extends Thread
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpClientTimeOutThread");

   private FtpClientSession clientSession;

   private int timeOutValue;

   private int clock = 0;

   public FtpClientTimeOutThread(FtpClientSession clientSession)
   {
      this.clientSession = clientSession;
      timeOutValue = clientSession.getFtpServer().getConfiguration().getTimeOut();
   }

   public void refreshTimeOut()
   {
      clock = 0;
   }

   public void run()
   {
      while (true)
      {
         try
         {
            Thread.sleep(1000);
            clock++;
            if (clock >= timeOutValue)
            {
               break;
            }
         }
         catch (InterruptedException iexc)
         {
            return;
         }
      }

      try
      {
         clientSession.reply(String.format(FtpConst.Replyes.REPLY_421, timeOutValue));
      }
      catch (IOException ioexc)
      {
         log.info("Unhandled exception. " + ioexc.getMessage(), ioexc);
      }
      clientSession.logout();
   }

}
